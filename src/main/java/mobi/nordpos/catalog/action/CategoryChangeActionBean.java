/**
 * Copyright (c) 2012-2014 Nord Trading Network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package mobi.nordpos.catalog.action;

import java.io.IOException;
import java.sql.SQLException;
import mobi.nordpos.dao.model.ProductCategory;
import mobi.nordpos.catalog.util.ImagePreview;
import mobi.nordpos.dao.ormlite.ProductCategoryPersist;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.StringTypeConverter;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;

/**
 * @author Andrey Svininykh <svininykh@gmail.com>
 */
public class CategoryChangeActionBean extends CategoryBaseActionBean {

    private static final String CATEGORY_EDIT = "/WEB-INF/jsp/category_edit.jsp";

    private ProductCategory currentCategory;

    private FileBean imageFile;

    @DefaultHandler
    public Resolution form() throws SQLException {
        return new ForwardResolution(CATEGORY_EDIT);
    }

    public Resolution update() {
        ProductCategory category = getCategory();
        try {
            ProductCategoryPersist pcPersist = new ProductCategoryPersist(getDataBaseConnection());
            if (pcPersist.change(category)) {
                getContext().getMessages().add(
                        new SimpleMessage(getLocalizationKey("message.ProductCategory.updated"),
                                category.getName()));
            }
        } catch (SQLException ex) {
            getContext().getValidationErrors().addGlobalError(
                    new SimpleError(ex.getMessage()));
            return getContext().getSourcePageResolution();
        }
        return new ForwardResolution(CategoryListActionBean.class);
    }

    public Resolution delete() throws SQLException {
        ProductCategory category = getCategory();
        try {
            ProductCategoryPersist pcPersist = new ProductCategoryPersist(getDataBaseConnection());
            if (pcPersist.delete(category.getId())) {
                getContext().getMessages().add(
                        new SimpleMessage(getLocalizationKey("message.ProductCategory.deleted"),
                                category.getName()));
            }
        } catch (SQLException ex) {
            getContext().getValidationErrors().addGlobalError(
                    new SimpleError(ex.getMessage()));
            return getContext().getSourcePageResolution();
        }
        return new ForwardResolution(CategoryListActionBean.class);
    }

    @ValidationMethod(on = "delete")
    public void validateProductListIsEmpty(ValidationErrors errors) throws SQLException {
        ProductCategoryPersist pcPersist = new ProductCategoryPersist(getDataBaseConnection());
        setCategory(pcPersist.read(getCategory().getId()));
        if (!getCategory().getProductCollection().isEmpty()) {
            errors.addGlobalError(new SimpleError(
                    getLocalizationKey("error.ProductCategory.IncludeProducts"), getCategory().getName(), getCategory().getProductCollection().size()
            ));
        }
    }

    @ValidationMethod(on = "update")
    public void validateCategoryNameIsUnique(ValidationErrors errors) {
        String name = getCategory().getName();
        if (name != null && !name.isEmpty() && !name.equals(currentCategory.getName())) {
            try {
                ProductCategoryPersist pcPersist = new ProductCategoryPersist(getDataBaseConnection());
                if (pcPersist.find(ProductCategory.NAME, name) != null) {
                    errors.add("category.name", new SimpleError(
                            getLocalizationKey("error.ProductCategory.AlreadyExists"), name));
                }
            } catch (SQLException ex) {
                getContext().getValidationErrors().addGlobalError(
                        new SimpleError(ex.getMessage()));
            }
        }
    }

    @ValidationMethod(on = "update")
    public void validateCategoryCodeIsUnique(ValidationErrors errors) {
        String code = getCategory().getCode();
        if (code != null && !code.isEmpty() && !code.equals(currentCategory.getCode())) {
            try {
                ProductCategoryPersist pcPersist = new ProductCategoryPersist(getDataBaseConnection());
                if (pcPersist.find(ProductCategory.CODE, code) != null) {
                    errors.add("category.code", new SimpleError(
                            getLocalizationKey("error.ProductCategory.AlreadyExists"), code));
                }
            } catch (SQLException ex) {
                getContext().getValidationErrors().addGlobalError(
                        new SimpleError(ex.getMessage()));
            }
        }
    }

    @ValidationMethod(on = "update")
    public void validateCategoryImageUpload(ValidationErrors errors) {
        try {
            ProductCategoryPersist pcPersist = new ProductCategoryPersist(getDataBaseConnection());
            if (imageFile != null) {
                if (imageFile.getContentType().startsWith("image")) {
                    try {
                        getCategory().setImage(ImagePreview.createThumbnail(imageFile.getInputStream(), 256));
                    } catch (IOException ex) {
                        errors.add("category.image", new SimpleError(
                                getLocalizationKey("error.ProductCategory.FileNotUpload"), imageFile.getFileName()));
                    }
                } else {
                    errors.add("category.image", new SimpleError(
                            getLocalizationKey("error.ProductCategory.FileNotImage"), imageFile.getFileName()));
                }
            } else {
                getCategory().setImage(pcPersist.read(getCategory().getId()).getImage());
            }
        } catch (SQLException ex) {
            getContext().getValidationErrors().addGlobalError(
                    new SimpleError(ex.getMessage()));
        }
    }

    @ValidationMethod(on = "form")
    public void validateCategoryIdIsAvalaible(ValidationErrors errors) {
        try {
            ProductCategoryPersist pcPersist = new ProductCategoryPersist(getDataBaseConnection());
            ProductCategory category = pcPersist.read(getCategory().getId());
            if (category != null) {
                setCategory(category);
            } else {
                errors.add("category.id", new SimpleError(
                        getLocalizationKey("error.CatalogNotInclude")));
            }
        } catch (SQLException ex) {
            getContext().getValidationErrors().addGlobalError(
                    new SimpleError(ex.getMessage()));
        }
    }

    @ValidateNestedProperties({
        @Validate(on = {"form", "update", "delete"},
                field = "id",
                required = true,
                converter = StringTypeConverter.class),
        @Validate(on = {"update"},
                field = "name",
                required = true,
                trim = true,
                maxlength = 255),
        @Validate(on = {"update"},
                field = "code",
                trim = true,
                maxlength = 4)
    })
    @Override
    public void setCategory(ProductCategory category) {
        super.setCategory(category);
    }

    public ProductCategory getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(ProductCategory currentCategory) {
        this.currentCategory = currentCategory;
    }

    public FileBean getImageFile() {
        return imageFile;
    }

    public void setImageFile(FileBean imageFile) {
        this.imageFile = imageFile;
    }
}

package com.angkorteam.ecommerce.page.shop;

import com.angkorteam.ecommerce.model.EcommerceShop;
import com.angkorteam.framework.extension.wicket.markup.html.form.Button;
import com.angkorteam.framework.extension.wicket.markup.html.form.Form;
import com.angkorteam.framework.extension.wicket.markup.html.panel.TextFeedbackPanel;
import com.angkorteam.framework.jdbc.SelectQuery;
import com.angkorteam.framework.jdbc.UpdateQuery;
import com.angkorteam.platform.Platform;
import com.angkorteam.platform.page.MBaaSPage;
import com.angkorteam.platform.validator.UniqueRecordValidator;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Created by socheatkhauv on 26/1/17.
 */
public class ShopModifyPage extends MBaaSPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModifyPage.class);

    private String ecommerceShopId;

    private String name;
    private TextField<String> nameField;
    private TextFeedbackPanel nameFeedback;

    private String description;
    private TextField<String> descriptionField;
    private TextFeedbackPanel descriptionFeedback;

    private String google;
    private TextField<String> googleField;
    private TextFeedbackPanel googleFeedback;

    private List<FileUpload> logo;
    private FileUploadField logoField;
    private TextFeedbackPanel logoFeedback;

    private String language;
    private TextField<String> languageField;
    private TextFeedbackPanel languageFeedback;

    private String url;
    private TextField<String> urlField;
    private TextFeedbackPanel urlFeedback;

    private List<FileUpload> flagIcon;
    private FileUploadField flagIconField;
    private TextFeedbackPanel flagIconFeedback;

    private Button saveButton;
    private BookmarkablePageLink<Void> closeButton;
    private Form<Void> form;

    @Override
    protected void doInitialize(Border layout) {
        add(layout);

        this.ecommerceShopId = getPageParameters().get("ecommerceShopId").toString("");

        SelectQuery selectQuery = null;
        selectQuery = new SelectQuery("ecommerce_shop");
        selectQuery.addWhere("ecommerce_shop_id = :ecommerce_shop_id", this.ecommerceShopId);

        EcommerceShop ecommerceShop = getNamed().queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceShop.class);

        this.form = new Form<>("form");
        layout.add(this.form);

        this.name = ecommerceShop.getName();
        this.nameField = new TextField<>("nameField", new PropertyModel<>(this, "name"));
        this.nameField.add(new UniqueRecordValidator<>("ecommerce_shop", "name", "ecommerce_shop_id", this.ecommerceShopId));
        this.nameField.setRequired(true);
        this.form.add(this.nameField);
        this.nameFeedback = new TextFeedbackPanel("nameFeedback", this.nameField);
        this.form.add(this.nameFeedback);

        this.description = ecommerceShop.getDescription();
        this.descriptionField = new TextField<>("descriptionField", new PropertyModel<>(this, "description"));
        this.form.add(this.descriptionField);
        this.descriptionFeedback = new TextFeedbackPanel("descriptionFeedback", this.descriptionField);
        this.form.add(this.descriptionFeedback);

        this.language = ecommerceShop.getLanguage();
        this.languageField = new TextField<>("languageField", new PropertyModel<>(this, "language"));
        this.languageField.setRequired(true);
        this.form.add(this.languageField);
        this.languageFeedback = new TextFeedbackPanel("languageFeedback", this.languageField);
        this.form.add(this.languageFeedback);

        this.logoField = new FileUploadField("logoField", new PropertyModel<>(this, "logo"));
        this.form.add(this.logoField);
        this.logoFeedback = new TextFeedbackPanel("logoFeedback", this.logoField);
        this.form.add(this.logoFeedback);

        this.google = ecommerceShop.getGoogleUa();
        this.googleField = new TextField<>("googleField", new PropertyModel<>(this, "google"));
        this.googleField.setRequired(true);
        this.form.add(this.googleField);
        this.googleFeedback = new TextFeedbackPanel("googleFeedback", this.googleField);
        this.form.add(this.googleFeedback);

        this.url = ecommerceShop.getUrl();
        this.urlField = new TextField<>("urlField", new PropertyModel<String>(this, "url"));
        this.form.add(this.urlField);
        this.urlFeedback = new TextFeedbackPanel("urlFeedback", this.urlField);
        this.form.add(this.urlFeedback);

        this.flagIconField = new FileUploadField("flagIconField", new PropertyModel<>(this, "flagIcon"));
        this.flagIconField.setRequired(true);
        this.form.add(this.flagIconField);
        this.flagIconFeedback = new TextFeedbackPanel("flagIconFeedback", this.flagIconField);
        this.form.add(this.flagIconFeedback);

        this.saveButton = new Button("saveButton");
        this.saveButton.setOnSubmit(this::saveButtonOnSubmit);
        this.form.add(this.saveButton);

        this.closeButton = new BookmarkablePageLink<>("closeButton", ShopBrowsePage.class);
        this.form.add(this.closeButton);
    }


    private void saveButtonOnSubmit(Button button) {
        Long flagIconFileId = null;
        if (this.flagIcon != null && !this.flagIcon.isEmpty()) {
            File file = new File(FileUtils.getTempDirectory(), Platform.randomUUIDString() + this.flagIcon.get(0).getClientFileName());
            try {
                this.flagIcon.get(0).writeTo(file);
            } catch (Exception e) {
                throw new WicketRuntimeException(e);
            }
            flagIconFileId = Platform.saveFile(file);
            file.delete();
        }

        Long logoFileId = null;
        if (this.logo != null && !this.logo.isEmpty() && this.logo.get(0).getSize() > 0) {
            File file = new File(FileUtils.getTempDirectory(), Platform.randomUUIDString() + this.logo.get(0).getClientFileName());
            try {
                this.logo.get(0).writeTo(file);
            } catch (Exception e) {
                throw new WicketRuntimeException(e);
            }
            logoFileId = Platform.saveFile(file);
            file.delete();
        }

        UpdateQuery updateQuery = new UpdateQuery("ecommerce_shop");
        updateQuery.addValue("name = :name", this.name);
        updateQuery.addValue("description = :description", this.description);
        updateQuery.addValue("language = :language", this.language);
        updateQuery.addValue("url = :url", this.url);
        updateQuery.addValue("google_ua = :google_ua", this.google);
        updateQuery.addValue("logo_platform_file_id = :logo_platform_file_id", logoFileId);
        updateQuery.addValue("flag_icon_platform_file_id = :flag_icon_platform_file_id", flagIconFileId);
        updateQuery.addWhere("ecommerce_shop_id = :ecommerce_shop_id", this.ecommerceShopId);
        getNamed().update(updateQuery.toSQL(), updateQuery.getParam());

        setResponsePage(ShopBrowsePage.class);
    }

}
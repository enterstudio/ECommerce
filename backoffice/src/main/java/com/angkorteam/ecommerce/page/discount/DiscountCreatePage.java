package com.angkorteam.ecommerce.page.discount;

import com.angkorteam.framework.extension.wicket.markup.html.form.Button;
import com.angkorteam.framework.extension.wicket.markup.html.form.Form;
import com.angkorteam.framework.extension.wicket.markup.html.panel.TextFeedbackPanel;
import com.angkorteam.framework.jdbc.InsertQuery;
import com.angkorteam.platform.Platform;
import com.angkorteam.platform.page.MBaaSPage;
import com.angkorteam.platform.validator.UniqueRecordValidator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by socheatkhauv on 25/1/17.
 */
public class DiscountCreatePage extends MBaaSPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscountCreatePage.class);

    private String name;
    private TextField<String> nameField;
    private TextFeedbackPanel nameFeedback;

    private String type;
    private TextField<String> typeField;
    private TextFeedbackPanel typeFeedback;

    private Double value;
    private TextField<Double> valueField;
    private TextFeedbackPanel valueFeedback;

    private Double minCartAmount = 0d;
    private TextField<Double> minCartAmountField;
    private TextFeedbackPanel minCartAmountFeedback;

    private Button saveButton;
    private BookmarkablePageLink<Void> closeButton;
    private Form<Void> form;

    @Override
    protected void doInitialize(Border layout) {
        add(layout);

        this.form = new Form<>("form");
        layout.add(this.form);

        this.name = StringUtils.upperCase(RandomStringUtils.randomAlphabetic(6));
        this.nameField = new TextField<>("nameField", new PropertyModel<>(this, "name"));
        this.nameField.add(new UniqueRecordValidator<>("ecommerce_discount", "name"));
        this.nameField.setRequired(true);
        this.form.add(this.nameField);
        this.nameFeedback = new TextFeedbackPanel("nameFeedback", this.nameField);
        this.form.add(this.nameFeedback);

        this.typeField = new TextField<>("typeField", new PropertyModel<>(this, "type"));
        this.typeField.setRequired(true);
        this.form.add(this.typeField);
        this.typeFeedback = new TextFeedbackPanel("typeFeedback", this.typeField);
        this.form.add(this.typeFeedback);

        this.valueField = new TextField<>("valueField", new PropertyModel<>(this, "value"));
        this.valueField.setRequired(true);
        this.form.add(this.valueField);
        this.valueFeedback = new TextFeedbackPanel("valueFeedback", this.valueField);
        this.form.add(this.valueFeedback);

        this.minCartAmountField = new TextField<>("minCartAmountField", new PropertyModel<>(this, "minCartAmount"));
        this.minCartAmountField.setRequired(true);
        this.form.add(this.minCartAmountField);
        this.minCartAmountFeedback = new TextFeedbackPanel("minCartAmountFeedback", this.minCartAmountField);
        this.form.add(this.minCartAmountFeedback);

        this.saveButton = new Button("saveButton");
        this.saveButton.setOnSubmit(this::saveButtonOnSubmit);
        this.form.add(this.saveButton);

        this.closeButton = new BookmarkablePageLink<>("closeButton", DiscountBrowsePage.class);
        this.form.add(this.closeButton);
    }

    private void saveButtonOnSubmit(Button button) {
        InsertQuery insertQuery = new InsertQuery("ecommerce_discount");
        insertQuery.addValue("ecommerce_discount_id = :ecommerce_discount_id", Platform.randomUUIDLong("ecommerce_discount"));
        insertQuery.addValue("name = :name", this.name);
        insertQuery.addValue("type = :type", this.type);
        insertQuery.addValue("value = :value", this.value);
        insertQuery.addValue("min_cart_amount = :min_cart_amount", this.minCartAmount);
        getNamed().update(insertQuery.toSQL(), insertQuery.getParam());
        setResponsePage(DiscountBrowsePage.class);
    }

}
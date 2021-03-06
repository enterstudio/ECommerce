package com.angkorteam.ecommerce.controller;

import com.angkorteam.ecommerce.mobile.order.Order;
import com.angkorteam.ecommerce.model.*;
import com.angkorteam.framework.jdbc.DeleteQuery;
import com.angkorteam.framework.jdbc.InsertQuery;
import com.angkorteam.framework.jdbc.SelectQuery;
import com.angkorteam.framework.jdbc.UpdateQuery;
import com.angkorteam.framework.spring.JdbcTemplate;
import com.angkorteam.framework.spring.NamedParameterJdbcTemplate;
import com.angkorteam.platform.Platform;
import com.angkorteam.platform.model.PlatformUser;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by socheatkhauv on 27/1/17.
 */
@Controller
public class OrdersServicePost {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrdersServicePost.class);

    @Autowired
    @Qualifier("gson")
    private Gson gson;

    @RequestMapping(path = "/{shop}/orders", method = RequestMethod.POST)
    public ResponseEntity<?> service(HttpServletRequest request) throws Throwable {
        JdbcTemplate jdbcTemplate = Platform.getBean(JdbcTemplate.class);
        NamedParameterJdbcTemplate named = Platform.getBean(NamedParameterJdbcTemplate.class);

        if (!Platform.hasAccess(request, OrdersServicePost.class)) {
            throw new ServletException(String.valueOf(HttpStatus.FORBIDDEN.getReasonPhrase()));
        }

        String asset = Platform.getSetting("asset");
        String currency = Platform.getSetting("currency");
        DecimalFormat priceFormat = new DecimalFormat(Platform.getSetting("price_format"));
        DateFormat datetimeFormat = new SimpleDateFormat(Platform.getSetting("datetime_format"));

        PlatformUser currentUser = Platform.getCurrentUser(request);

        SelectQuery selectQuery = null;

        RequestBody requestBody = this.gson.fromJson(request.getReader(), RequestBody.class);

        LOGGER.info(this.gson.toJson(requestBody));

        selectQuery = new SelectQuery("ecommerce_shipping");
        selectQuery.addWhere("ecommerce_shipping_id = :ecommerce_shipping_id", requestBody.shippingType);
        EcommerceShipping shippingRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceShipping.class);

        selectQuery = new SelectQuery("ecommerce_payment");
        selectQuery.addWhere("ecommerce_payment_id = :ecommerce_payment_id", requestBody.paymentType);
        EcommercePayment paymentRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommercePayment.class);

        String ecommerceRegionId = null;
        Date createdDate = new Date();

        // new, in_process, completed, canceled
        String status = "New";

        selectQuery = new SelectQuery("ecommerce_cart");
        selectQuery.addWhere("platform_user_id = :platform_user_id", currentUser.getPlatformUserId());
        EcommerceCart cartRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceCart.class);

        Integer totalProductCount = 0;
        Double totalPrice = 0d;

        selectQuery = new SelectQuery("ecommerce_cart_product_item");
        selectQuery.addWhere("ecommerce_cart_id = :ecommerce_cart_id", cartRecord.getEcommerceCartId());
        List<EcommerceCartProductItem> cartItemRecords = named.queryForList(selectQuery.toSQL(), selectQuery.getParam(), EcommerceCartProductItem.class);

        Map<Long, Integer> requestVariants = Maps.newHashMap();
        if (cartItemRecords != null && !cartItemRecords.isEmpty()) {
            for (EcommerceCartProductItem cartItemRecord : cartItemRecords) {
                Long variantId = cartItemRecord.getEcommerceProductVariantId();
                if (!requestVariants.containsKey(variantId)) {
                    requestVariants.put(variantId, 0);
                }
                Integer quantity = cartItemRecord.getQuantity();
                quantity = quantity + requestVariants.get(variantId);
                requestVariants.put(variantId, quantity);
            }
        }

        // stock validation
        List<String> errors = Lists.newArrayList();
        for (Map.Entry<Long, Integer> requestQuantity : requestVariants.entrySet()) {
            selectQuery = new SelectQuery("ecommerce_product_variant");
            selectQuery.addWhere("ecommerce_product_variant_id = :ecommerce_product_variant_id", requestQuantity.getKey());
            EcommerceProductVariant variantRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceProductVariant.class);

            selectQuery = new SelectQuery("ecommerce_product");
            selectQuery.addWhere("ecommerce_product_id = :ecommerce_product_id", variantRecord.getEcommerceProductId());
            EcommerceProduct productRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceProduct.class);

            selectQuery = new SelectQuery("ecommerce_color");
            selectQuery.addWhere("ecommerce_color_id = :ecommerce_color_id", variantRecord.getEcommerceColorId());
            EcommerceColor colorRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceColor.class);

            selectQuery = new SelectQuery("ecommerce_size");
            selectQuery.addWhere("ecommerce_size_id = :ecommerce_size_id", variantRecord.getEcommerceSizeId());
            EcommerceSize sizeRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceSize.class);

            Integer quantity = variantRecord.getQuantity() == null ? 0 : variantRecord.getQuantity();
            if (quantity <= 0) {
                String productName = productRecord.getName();
                String colorValue = colorRecord.getValue();
                String sizeValue = sizeRecord.getValue();
                errors.add(productName + " is out of stock for " + colorValue + " / " + sizeValue);
            } else {
                if (quantity < requestQuantity.getValue()) {
                    String productName = productRecord.getName();
                    String colorValue = colorRecord.getValue();
                    String sizeValue = sizeRecord.getValue();
                    errors.add(productName + " is available only " + quantity + " for " + colorValue + " / " + sizeValue);
                }
            }
        }

        if (!errors.isEmpty()) {
            Map<String, Object> error = Maps.newHashMap();
            error.put("body", errors);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        InsertQuery insertQuery = null;
        insertQuery = new InsertQuery("ecommerce_order");

        Double shippingPriceAddon = 0d;

        Long orderId = Platform.randomUUIDLong("ecommerce_order");
        insertQuery.addValue("ecommerce_order_id = :ecommerce_order_id", orderId);
        insertQuery.addValue("platform_user_id = :platform_user_id", currentUser.getPlatformUserId());
        insertQuery.addValue("ecommerce_shipping_id = :ecommerce_shipping_id", shippingRecord.getEcommerceShippingId());
        insertQuery.addValue("ecommerce_payment_id = :ecommerce_payment_id", paymentRecord.getEcommercePaymentId());
        insertQuery.addValue("name = :name", requestBody.name);
        insertQuery.addValue("street = :street", requestBody.street);
        insertQuery.addValue("house_number = :house_number", requestBody.houseNumber);
        insertQuery.addValue("city = :city", requestBody.city);
        insertQuery.addValue("zip = :zip", requestBody.zip);
        insertQuery.addValue("email = :email", requestBody.email);
        insertQuery.addValue("phone = :phone", requestBody.phone);
        insertQuery.addValue("note = :note", requestBody.note);
        insertQuery.addValue("date_created = :date_created", createdDate);
        insertQuery.addValue("buyer_status = :buyer_status", "Reviewing");
        insertQuery.addValue("order_status = :order_status", status);
        insertQuery.addValue("total = :total", 0);
        insertQuery.addValue("shipping_name = :shipping_name", shippingRecord.getName());
        insertQuery.addValue("shipping_price = :shipping_price", 0);
        insertQuery.addValue("payment_name = :payment_name", paymentRecord.getName());
        insertQuery.addValue("payment_price = :payment_price", paymentRecord.getPrice());

        shippingPriceAddon = shippingRecord.getPrice();

        named.update(insertQuery.toSQL(), insertQuery.getParam());

        if (cartItemRecords != null && !cartItemRecords.isEmpty()) {
            for (EcommerceCartProductItem cartItemRecord : cartItemRecords) {

                // create master order detail

                selectQuery = new SelectQuery("ecommerce_product");
                selectQuery.addWhere("ecommerce_product_id = :ecommerce_product_id", cartItemRecord.getEcommerceProductId());
                EcommerceProduct productRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceProduct.class);

                selectQuery = new SelectQuery("ecommerce_product_variant");
                selectQuery.addWhere("ecommerce_product_variant_id = :ecommerce_product_variant_id", cartItemRecord.getEcommerceProductVariantId());
                EcommerceProductVariant variantRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceProductVariant.class);

                selectQuery = new SelectQuery("ecommerce_category");
                selectQuery.addWhere("ecommerce_category_id = :ecommerce_category_id", productRecord.getEcommerceCategoryId());
                EcommerceCategory categoryRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceCategory.class);

                selectQuery = new SelectQuery("ecommerce_color");
                selectQuery.addWhere("ecommerce_color_id = :ecommerce_color_id", variantRecord.getEcommerceColorId());
                EcommerceColor colorRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceColor.class);

                selectQuery = new SelectQuery("ecommerce_size");
                selectQuery.addWhere("ecommerce_size_id = :ecommerce_size_id", variantRecord.getEcommerceSizeId());
                EcommerceSize sizeRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceSize.class);

                Integer quantity = cartItemRecord.getQuantity() == null ? 0 : cartItemRecord.getQuantity();
                Double price = productRecord.getPrice() == null ? 0d : productRecord.getPrice();
                totalProductCount = quantity + totalProductCount;
                totalPrice = totalPrice + (quantity * price);
                Double productShippingPriceAddon = productRecord.getShippingPrice() == null ? 0d : productRecord.getShippingPrice();
                if (productShippingPriceAddon != null && productShippingPriceAddon > 0) {
                    shippingPriceAddon = (shippingPriceAddon == null ? 0d : shippingPriceAddon) + (productShippingPriceAddon * quantity);
                }

                selectQuery = new SelectQuery("platform_file");
                selectQuery.addField("CONCAT('" + asset + "', '/api/resource', platform_file.path, '/', platform_file.name)");
                selectQuery.addWhere("platform_file.platform_file_id = :platform_file_id", productRecord.getMainImagePlatformFileId());
                String mainImage = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), String.class);

                selectQuery = new SelectQuery("platform_file");
                selectQuery.addField("CONCAT('" + asset + "', '/api/resource', platform_file.path, '/', platform_file.name)");
                selectQuery.addWhere("platform_file.platform_file_id = :platform_file_id", colorRecord.getImgPlatformFileId());
                String img = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), String.class);

                insertQuery = new InsertQuery("ecommerce_order_item");
                insertQuery.addValue("ecommerce_order_item_id = :ecommerce_order_item_id", Platform.randomUUIDLong("ecommerce_order_item"));
                insertQuery.addValue("ecommerce_order_id = :ecommerce_order_id", orderId);
                insertQuery.addValue("quantity = :quantity", cartItemRecord.getQuantity());
                insertQuery.addValue("total_price = :total_price", quantity * price);
                insertQuery.addValue("ecommerce_category_id = :ecommerce_category_id", categoryRecord.getEcommerceCategoryId());
                insertQuery.addValue("ecommerce_product_id = :ecommerce_product_id", productRecord.getEcommerceProductId());
                insertQuery.addValue("product_url = :product_url", productRecord.getUrl());
                insertQuery.addValue("product_name = :product_name", productRecord.getName());
                insertQuery.addValue("product_shipping_price = :product_shipping_price", productRecord.getShippingPrice());
                insertQuery.addValue("product_price = :product_price", productRecord.getPrice());
                insertQuery.addValue("product_reference = :product_reference", productRecord.getReference());
                insertQuery.addValue("product_discount_price = :product_discount_price", productRecord.getDiscountPrice());
                insertQuery.addValue("product_description = :product_description", productRecord.getDescription());
                insertQuery.addValue("product_main_image = :product_main_image", mainImage);
                insertQuery.addValue("product_main_image_platform_file_id = :product_main_image_platform_file_id", productRecord.getMainImagePlatformFileId());
                insertQuery.addValue("ecommerce_product_variant_id = :ecommerce_product_variant_id", variantRecord.getEcommerceProductId());
                insertQuery.addValue("variant_reference = :variant_reference", variantRecord.getReference());
                insertQuery.addValue("ecommerce_color_id = :ecommerce_color_id", colorRecord.getEcommerceColorId());
                insertQuery.addValue("color_value = :color_value", colorRecord.getValue());
                insertQuery.addValue("color_code = :color_code", colorRecord.getCode());
                insertQuery.addValue("color_reference = :color_reference", colorRecord.getReference());
                insertQuery.addValue("color_img = :color_img", img);
                insertQuery.addValue("color_img_platform_file_id = :color_img_platform_file_id", colorRecord.getImgPlatformFileId());
                insertQuery.addValue("ecommerce_size_id = :ecommerce_size_id", sizeRecord.getEcommerceSizeId());
                insertQuery.addValue("size_value = :size_value", sizeRecord.getValue());
                insertQuery.addValue("size_reference = :size_reference", sizeRecord.getReference());
                named.update(insertQuery.toSQL(), insertQuery.getParam());
            }
        }

        List<Long> productIds = Lists.newArrayList();

        UpdateQuery updateQuery = null;

        // Stock Reduce Variant
        for (Map.Entry<Long, Integer> requestVariant : requestVariants.entrySet()) {
            selectQuery = new SelectQuery("ecommerce_product_variant");
            selectQuery.addWhere("ecommerce_product_variant_id = :ecommerce_product_variant_id", requestVariant.getKey());
            EcommerceProductVariant variantRecord = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), EcommerceProductVariant.class);
            Long productId = variantRecord.getEcommerceProductId();
            if (!productIds.contains(productId)) {
                productIds.add(productId);
            }
            Integer quantity = variantRecord.getQuantity();
            Integer remainQuantity = (quantity == null ? 0 : quantity) - requestVariant.getValue();
            updateQuery = new UpdateQuery("ecommerce_product_variant");
            updateQuery.addValue("quantity = :quantity", remainQuantity);
            updateQuery.addWhere("ecommerce_product_variant_id = :ecommerce_product_variant_id", requestVariant.getKey());
            named.update(updateQuery.toSQL(), updateQuery.getParam());
        }

        // Product Stock Re-Calculation
        for (Long productId : productIds) {
            selectQuery = new SelectQuery("ecommerce_product_variant");
            selectQuery.addField("sum(quantity)");
            selectQuery.addWhere("ecommerce_product_id = :ecommerce_product_id", productId);
            Integer quantity = named.queryForObject(selectQuery.toSQL(), selectQuery.getParam(), int.class);
            updateQuery = new UpdateQuery("ecommerce_product");
            updateQuery.addValue("quantity = :quantity", quantity);
            updateQuery.addWhere("ecommerce_product_id = :ecommerce_product_id", productId);
            named.update(updateQuery.toSQL(), updateQuery.getParam());
        }

        updateQuery = new UpdateQuery("ecommerce_order");
        updateQuery.addValue("shipping_price = :shipping_price", shippingPriceAddon);
        updateQuery.addValue("total = :total", totalPrice);
        updateQuery.addWhere("ecommerce_order_id = :ecommerce_order_id", orderId);
        named.update(updateQuery.toSQL(), updateQuery.getParam());

        insertQuery = new InsertQuery("ecommerce_cart");
        insertQuery.addValue("ecommerce_cart_id = :ecommerce_cart_id", Platform.randomUUIDLong("ecommerce_cart"));
        insertQuery.addValue("platform_user_id = :platform_user_id", currentUser.getPlatformUserId());
        named.update(insertQuery.toSQL(), insertQuery.getParam());

        DeleteQuery deleteQuery = null;

        deleteQuery = new DeleteQuery("ecommerce_cart");
        deleteQuery.addWhere("ecommerce_cart_id = :ecommerce_cart_id", cartRecord.getEcommerceCartId());
        named.update(deleteQuery.toSQL(), deleteQuery.getParam());

        deleteQuery = new DeleteQuery("ecommerce_cart_product_item");
        deleteQuery.addWhere("ecommerce_cart_id = :ecommerce_cart_id", cartRecord.getEcommerceCartId());
        named.update(deleteQuery.toSQL(), deleteQuery.getParam());

        Order data = new Order();

        data.setName(requestBody.getName());
        data.setEmail(requestBody.getEmail());
        data.setPhone(requestBody.getPhone());
        data.setCity(requestBody.getCity());
        data.setStreet(requestBody.getStreet());
        data.setZip(requestBody.getZip());
        data.setHouseNumber(requestBody.getHouseNumber());

        return ResponseEntity.ok(data);
    }

    public static class RequestBody {

        @Expose
        @SerializedName("shipping_type")
        private Long shippingType;

        @Expose
        @SerializedName("payment_type")
        private Long paymentType;

        @Expose
        @SerializedName("name")
        private String name;

        @Expose
        @SerializedName("street")
        private String street;

        @Expose
        @SerializedName("house_number")
        private String houseNumber;

        @Expose
        @SerializedName("city")
        private String city;

        @Expose
        @SerializedName("zip")
        private String zip;

        @Expose
        @SerializedName("email")
        private String email;

        @Expose
        @SerializedName("phone")
        private String phone;

        @Expose
        @SerializedName("note")
        private String note;

        public Long getShippingType() {
            return shippingType;
        }

        public void setShippingType(Long shippingType) {
            this.shippingType = shippingType;
        }

        public Long getPaymentType() {
            return paymentType;
        }

        public void setPaymentType(Long paymentType) {
            this.paymentType = paymentType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getHouseNumber() {
            return houseNumber;
        }

        public void setHouseNumber(String houseNumber) {
            this.houseNumber = houseNumber;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

}

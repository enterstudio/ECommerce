package com.angkorteam.ecommerce.model;

import java.io.Serializable;

/**
 * Created by socheatkhauv on 31/1/17.
 */
public class EcommerceCartProductItem implements Serializable {

    private Long ecommerceCartProductItemId;
    private Long ecommerceProductId;
    private Long ecommerceProductVariantId;
    private Long ecommerceCartId;
    private Integer quantity;

    public Long getEcommerceCartProductItemId() {
        return ecommerceCartProductItemId;
    }

    public void setEcommerceCartProductItemId(Long ecommerceCartProductItemId) {
        this.ecommerceCartProductItemId = ecommerceCartProductItemId;
    }

    public Long getEcommerceProductId() {
        return ecommerceProductId;
    }

    public void setEcommerceProductId(Long ecommerceProductId) {
        this.ecommerceProductId = ecommerceProductId;
    }

    public Long getEcommerceProductVariantId() {
        return ecommerceProductVariantId;
    }

    public void setEcommerceProductVariantId(Long ecommerceProductVariantId) {
        this.ecommerceProductVariantId = ecommerceProductVariantId;
    }

    public Long getEcommerceCartId() {
        return ecommerceCartId;
    }

    public void setEcommerceCartId(Long ecommerceCartId) {
        this.ecommerceCartId = ecommerceCartId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EcommerceCartProductItem that = (EcommerceCartProductItem) o;

        if (ecommerceCartProductItemId != null ? !ecommerceCartProductItemId.equals(that.ecommerceCartProductItemId) : that.ecommerceCartProductItemId != null)
            return false;
        if (ecommerceProductId != null ? !ecommerceProductId.equals(that.ecommerceProductId) : that.ecommerceProductId != null)
            return false;
        if (ecommerceProductVariantId != null ? !ecommerceProductVariantId.equals(that.ecommerceProductVariantId) : that.ecommerceProductVariantId != null)
            return false;
        if (ecommerceCartId != null ? !ecommerceCartId.equals(that.ecommerceCartId) : that.ecommerceCartId != null)
            return false;
        return quantity != null ? quantity.equals(that.quantity) : that.quantity == null;
    }

    @Override
    public int hashCode() {
        int result = ecommerceCartProductItemId != null ? ecommerceCartProductItemId.hashCode() : 0;
        result = 31 * result + (ecommerceProductId != null ? ecommerceProductId.hashCode() : 0);
        result = 31 * result + (ecommerceProductVariantId != null ? ecommerceProductVariantId.hashCode() : 0);
        result = 31 * result + (ecommerceCartId != null ? ecommerceCartId.hashCode() : 0);
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        return result;
    }
}

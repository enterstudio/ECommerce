package com.angkorteam.ecommerce.model;

import java.io.Serializable;

/**
 * Created by socheatkhauv on 31/1/17.
 */
public class EcommerceWishlist implements Serializable {

    private Long ecommerceWishlistId;
    private Long platformUserId;
    private Long ecommerceProductId;

    public Long getEcommerceWishlistId() {
        return ecommerceWishlistId;
    }

    public void setEcommerceWishlistId(Long ecommerceWishlistId) {
        this.ecommerceWishlistId = ecommerceWishlistId;
    }

    public Long getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(Long platformUserId) {
        this.platformUserId = platformUserId;
    }

    public Long getEcommerceProductId() {
        return ecommerceProductId;
    }

    public void setEcommerceProductId(Long ecommerceProductId) {
        this.ecommerceProductId = ecommerceProductId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EcommerceWishlist that = (EcommerceWishlist) o;

        if (ecommerceWishlistId != null ? !ecommerceWishlistId.equals(that.ecommerceWishlistId) : that.ecommerceWishlistId != null)
            return false;
        if (platformUserId != null ? !platformUserId.equals(that.platformUserId) : that.platformUserId != null)
            return false;
        return ecommerceProductId != null ? ecommerceProductId.equals(that.ecommerceProductId) : that.ecommerceProductId == null;
    }

    @Override
    public int hashCode() {
        int result = ecommerceWishlistId != null ? ecommerceWishlistId.hashCode() : 0;
        result = 31 * result + (platformUserId != null ? platformUserId.hashCode() : 0);
        result = 31 * result + (ecommerceProductId != null ? ecommerceProductId.hashCode() : 0);
        return result;
    }
}

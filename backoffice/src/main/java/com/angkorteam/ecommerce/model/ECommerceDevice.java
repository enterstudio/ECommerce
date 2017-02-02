package com.angkorteam.ecommerce.model;

import java.io.Serializable;

/**
 * Created by socheatkhauv on 31/1/17.
 */
public class ECommerceDevice implements Serializable {

    private Long ecommerceDeviceId;
    private Long userId;
    private String deviceToken;
    private String platform;

    public Long getECommerceDeviceId() {
        return ecommerceDeviceId;
    }

    public void setECommerceDeviceId(Long ecommerceDeviceId) {
        this.ecommerceDeviceId = ecommerceDeviceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ECommerceDevice that = (ECommerceDevice) o;

        if (ecommerceDeviceId != null ? !ecommerceDeviceId.equals(that.ecommerceDeviceId) : that.ecommerceDeviceId != null)
            return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (deviceToken != null ? !deviceToken.equals(that.deviceToken) : that.deviceToken != null) return false;
        return platform != null ? platform.equals(that.platform) : that.platform == null;
    }

    @Override
    public int hashCode() {
        int result = ecommerceDeviceId != null ? ecommerceDeviceId.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (deviceToken != null ? deviceToken.hashCode() : 0);
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        return result;
    }
}
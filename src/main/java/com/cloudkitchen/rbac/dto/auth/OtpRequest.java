package com.cloudkitchen.rbac.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OtpRequest {
    private Integer merchantId;
    private String phone;

}

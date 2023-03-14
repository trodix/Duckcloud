package com.trodix.documentstorage.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * See https://api.onlyoffice.com/editors/callback
 */
@Data
public class OnlyOfficeCallbackRequest {

    private List<OnlyOfficeAction> actions;

    private String changeurl;

    private String filetype;

    private OnlyOfficeForceSaveType forcesavetype;

    private OnlyOfficeDocumentHistory history;

    //@NotEmpty
    private String key;

    //@NotEmpty
    private OnlyOfficeDocumentStatus status;

    private String url;

    private String userdata;

    private List<String> users;

}

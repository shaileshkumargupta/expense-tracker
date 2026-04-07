package com.expense_tracker.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@Data
@ToString(callSuper = true)
public class Response extends CommonResponse implements Serializable {

    private Map<String, Object> response;
}

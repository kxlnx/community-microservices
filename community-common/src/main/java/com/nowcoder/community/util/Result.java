package com.nowcoder.community.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一 API 响应类
 */
public class Result {

    private int code;
    private String msg;
    private Map<String, Object> data;

    private Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = new HashMap<>();
    }

    public static Result ok() {
        return new Result(0, "success");
    }

    public static Result ok(String msg) {
        return new Result(0, msg);
    }

    public static Result success(Map<String, Object> data) {
        Result result = new Result(0, "success");
        if (data != null) {
            result.data = data;
        }
        return result;
    }

    public static Result fail(int code, String msg) {
        return new Result(code, msg);
    }

    public Result put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}

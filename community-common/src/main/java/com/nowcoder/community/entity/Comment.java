package com.nowcoder.community.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

@Data
public class Comment {

    private int id;
    private int userId;
    private int entityType;
    private int entityId;
    private int targetId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private int status;
    private Date createTime;

}

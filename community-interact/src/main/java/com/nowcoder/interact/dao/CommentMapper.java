package com.nowcoder.interact.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId, @Param("offset") int offset, @Param("limit") int limit);

    int selectCountByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId);

    int insertComment(Comment comment);

    Comment selectCommentById(int id);

    List<Comment> selectCommentsByUserId(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    int selectCountByUserId(@Param("userId") int userId);

    List<Map<String, Object>> selectCountsByEntities(@Param("entityType") int entityType, @Param("entityIds") List<Integer> entityIds);

}

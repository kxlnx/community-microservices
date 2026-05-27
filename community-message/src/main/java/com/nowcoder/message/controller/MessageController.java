package com.nowcoder.message.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.feign.UserClient;
import com.nowcoder.message.service.MessageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
@Validated
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserClient userClient;

    // 私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(@RequestHeader("X-User-Id") int userId, Model model, Page page) {
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(userId));

        // 会话列表
        List<Message> conversationList = messageService.findConversations(
                userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null && !conversationList.isEmpty()) {
            // 批量收集target用户ID
            List<Integer> targetIds = new ArrayList<>();
            for (Message message : conversationList) {
                int targetId = userId == message.getFromId() ? message.getToId() : message.getFromId();
                targetIds.add(targetId);
            }
            Map<Integer, User> userMap = userClient.findUsersByIds(targetIds);

            for (int i = 0; i < conversationList.size(); i++) {
                Message message = conversationList.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount", messageService.findLetterUnreadCount(userId, message.getConversationId()));
                map.put("target", userMap.get(targetIds.get(i)));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        // 查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(userId, null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(userId, null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        model.addAttribute("loginUser", userClient.findUserById(userId));

        return "/site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@RequestHeader("X-User-Id") int userId,
                                  @PathVariable("conversationId") String conversationId,
                                  Page page, Model model) {
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null && !letterList.isEmpty()) {
            // 批量查询发送者用户
            List<Integer> fromIds = new ArrayList<>();
            for (Message message : letterList) {
                fromIds.add(message.getFromId());
            }
            Map<Integer, User> userMap = userClient.findUsersByIds(fromIds);

            for (int i = 0; i < letterList.size(); i++) {
                Message message = letterList.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userMap.get(fromIds.get(i)));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);

        // 私信目标
        model.addAttribute("target", getLetterTarget(conversationId, userId));

        // 设置已读
        List<Integer> ids = getLetterIds(letterList, userId);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        model.addAttribute("loginUser", userClient.findUserById(userId));
        return "/site/letter-detail";
    }

    private User getLetterTarget(String conversationId, int userId) {
        String[] ids = conversationId.split("_");
        if (ids.length < 2) {
            return null;
        }
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);

        if (userId == id0) {
            return userClient.findUserById(id1);
        } else {
            return userClient.findUserById(id0);
        }
    }

    private List<Integer> getLetterIds(List<Message> letterList, int userId) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                // 筛选条件：当前用户是接收者 + 消息未读
                if (userId == message.getToId() && message.getStatus() == 0) {
                    // 收集未读消息 ID
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(@RequestHeader("X-User-Id") int userId,
                             @NotBlank(message = "用户名不能为空!") String toName,
                             @NotBlank(message = "内容不能为空!") String content) {
        User target = userClient.findUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }

        Message message = new Message();
        message.setFromId(userId);
        message.setToId(target.getId());
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(@RequestHeader("X-User-Id") int userId, Model model) {
        // 收集所有通知中涉及的用户ID，用于批量查询
        Set<Integer> userIdSet = new HashSet<>();

        // 查询评论类通知
        Message message = messageService.findLatestNotice(userId, TOPIC_COMMENT);
        if (message != null) {
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            userIdSet.add((Integer) data.get("userId"));
        }

        // 查询点赞类通知
        Message likeMsg = messageService.findLatestNotice(userId, TOPIC_LIKE);
        if (likeMsg != null) {
            String content = HtmlUtils.htmlUnescape(likeMsg.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            userIdSet.add((Integer) data.get("userId"));
        }

        // 查询关注类通知
        Message followMsg = messageService.findLatestNotice(userId, TOPIC_FOLLOW);
        if (followMsg != null) {
            String content = HtmlUtils.htmlUnescape(followMsg.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            userIdSet.add((Integer) data.get("userId"));
        }

        // 批量查询用户
        Map<Integer, User> userMap = userClient.findUsersByIds(new ArrayList<>(userIdSet));

        // 评论类通知
        if (message != null) {
            Map<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userMap.get((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(userId, TOPIC_COMMENT);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(userId, TOPIC_COMMENT);
            messageVO.put("unread", unread);

            model.addAttribute("commentNotice", messageVO);
        }

        // 查询点赞类通知
        if (likeMsg != null) {
            Map<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", likeMsg);

            String content = HtmlUtils.htmlUnescape(likeMsg.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userMap.get((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(userId, TOPIC_LIKE);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(userId, TOPIC_LIKE);
            messageVO.put("unread", unread);

            model.addAttribute("likeNotice", messageVO);
        }

        // 查询关注类通知
        if (followMsg != null) {
            Map<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", followMsg);

            String content = HtmlUtils.htmlUnescape(followMsg.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userMap.get((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));

            int count = messageService.findNoticeCount(userId, TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(userId, TOPIC_FOLLOW);
            messageVO.put("unread", unread);

            model.addAttribute("followNotice", messageVO);
        }

        // 查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(userId, null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(userId, null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        model.addAttribute("loginUser", userClient.findUserById(userId));

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@RequestHeader("X-User-Id") int userId,
                                  @PathVariable("topic") String topic,
                                  Page page, Model model) {
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(userId, topic));

        List<Message> noticeList = messageService.findNotices(userId, topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null && !noticeList.isEmpty()) {
            // 收集所有涉及的用户ID
            Set<Integer> userIdSet = new HashSet<>();
            for (Message notice : noticeList) {
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                userIdSet.add((Integer) data.get("userId"));
                userIdSet.add(notice.getFromId());
            }
            Map<Integer, User> userMap = userClient.findUsersByIds(new ArrayList<>(userIdSet));

            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                map.put("notice", notice);
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userMap.get((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                map.put("fromUser", userMap.get(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(noticeList, userId);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        model.addAttribute("loginUser", userClient.findUserById(userId));
        return "/site/notice-detail";
    }

}

package com.gamevault.univibe;

public class Notificacion {
    private String notifId;
    private String targetUid;
    private String fromUid;
    private String fromUserName;
    private String fromUserPhotoUrl;
    private String type; // FOLLOW, LIKE_POST, LIKE_COMMENT, LIKE_REPLY, COMMENT_POST, REPLY_COMMENT
    private String postId;
    private String commentId;
    private String replyId;
    private String message;
    private long timestamp;
    private boolean read;

    public Notificacion() {
        // Required empty constructor for Firebase DataSnapshot.getValue(Notificacion.class)
    }

    public Notificacion(String notifId, String targetUid, String fromUid, String fromUserName, String fromUserPhotoUrl, String type, String postId, String commentId, String replyId, String message, long timestamp, boolean read) {
        this.notifId = notifId;
        this.targetUid = targetUid;
        this.fromUid = fromUid;
        this.fromUserName = fromUserName;
        this.fromUserPhotoUrl = fromUserPhotoUrl;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.replyId = replyId;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getNotifId() { return notifId != null ? notifId : ""; }
    public void setNotifId(String notifId) { this.notifId = notifId; }

    public String getTargetUid() { return targetUid != null ? targetUid : ""; }
    public void setTargetUid(String targetUid) { this.targetUid = targetUid; }

    public String getFromUid() { return fromUid != null ? fromUid : ""; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getFromUserName() { return fromUserName != null ? fromUserName : ""; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getFromUserPhotoUrl() { return fromUserPhotoUrl != null ? fromUserPhotoUrl : ""; }
    public void setFromUserPhotoUrl(String fromUserPhotoUrl) { this.fromUserPhotoUrl = fromUserPhotoUrl; }

    public String getType() { return type != null ? type : ""; }
    public void setType(String type) { this.type = type; }

    public String getPostId() { return postId != null ? postId : ""; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getCommentId() { return commentId != null ? commentId : ""; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getReplyId() { return replyId != null ? replyId : ""; }
    public void setReplyId(String replyId) { this.replyId = replyId; }

    public String getMessage() { return message != null ? message : ""; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}

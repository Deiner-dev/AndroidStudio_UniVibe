package com.gamevault.univibe;

public class Respuesta {
    private String replyId;
    private String commentId;
    private String uid;
    private String userName;
    private String userPhotoUrl;
    private String replyText;
    private String imageUrl;
    private long timestamp;
    private int likesCount;

    public Respuesta() {
        // Required empty constructor for Firebase DataSnapshot.getValue(Respuesta.class)
    }

    public Respuesta(String replyId, String commentId, String uid, String userName, String userPhotoUrl, String replyText, String imageUrl, long timestamp, int likesCount) {
        this.replyId = replyId;
        this.commentId = commentId;
        this.uid = uid;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.replyText = replyText;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
    }

    public String getReplyId() { return replyId != null ? replyId : ""; }
    public void setReplyId(String replyId) { this.replyId = replyId; }

    public String getCommentId() { return commentId != null ? commentId : ""; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getUid() { return uid != null ? uid : ""; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUserName() { return userName != null ? userName : ""; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhotoUrl() { return userPhotoUrl != null ? userPhotoUrl : ""; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public String getReplyText() { return replyText != null ? replyText : ""; }
    public void setReplyText(String replyText) { this.replyText = replyText; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}

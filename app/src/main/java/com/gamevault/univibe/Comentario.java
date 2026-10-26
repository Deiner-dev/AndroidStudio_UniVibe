package com.gamevault.univibe;

public class Comentario {
    private String commentId;
    private String postId;
    private String uid;
    private String userName;
    private String userPhotoUrl;
    private String commentText;
    private String imageUrl;
    private long timestamp;
    private int likesCount;

    public Comentario() {
        // Required empty constructor for Firebase DataSnapshot.getValue(Comentario.class)
    }

    public Comentario(String commentId, String postId, String uid, String userName, String userPhotoUrl, String commentText, String imageUrl, long timestamp, int likesCount) {
        this.commentId = commentId;
        this.postId = postId;
        this.uid = uid;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.commentText = commentText;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
    }

    public String getCommentId() { return commentId != null ? commentId : ""; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId != null ? postId : ""; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid != null ? uid : ""; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUserName() { return userName != null ? userName : ""; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhotoUrl() { return userPhotoUrl != null ? userPhotoUrl : ""; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public String getCommentText() { return commentText != null ? commentText : ""; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}

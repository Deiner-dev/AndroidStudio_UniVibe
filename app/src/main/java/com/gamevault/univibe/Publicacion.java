package com.gamevault.univibe;

public class Publicacion {
    private String postId;
    private String uid;
    private String userName;
    private String userPhotoUrl;
    private String university;
    private String career;
    private String semester;
    private String textContent;
    private String imageUrl;
    private String location;
    private long timestamp;
    private int commentsCount;
    private int likesCount;

    public Publicacion() {
        // Required empty constructor for Firebase DataSnapshot.getValue(Publicacion.class)
    }

    public Publicacion(String postId, String uid, String userName, String userPhotoUrl, String university,
                       String career, String semester, String textContent, String imageUrl,
                       String location, long timestamp) {
        this.postId = postId;
        this.uid = uid;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.university = university;
        this.career = career;
        this.semester = semester;
        this.textContent = textContent;
        this.imageUrl = imageUrl;
        this.location = location;
        this.timestamp = timestamp;
        this.commentsCount = 0;
        this.likesCount = 0;
    }

    public String getPostId() { return postId != null ? postId : ""; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid != null ? uid : ""; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUserName() { return userName != null ? userName : ""; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhotoUrl() { return userPhotoUrl != null ? userPhotoUrl : ""; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public String getUniversity() { return university != null ? university : ""; }
    public void setUniversity(String university) { this.university = university; }

    public String getCareer() { return career != null ? career : ""; }
    public void setCareer(String career) { this.career = career; }

    public String getSemester() { return semester != null ? semester : ""; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getTextContent() { return textContent != null ? textContent : ""; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLocation() { return location != null ? location : ""; }
    public void setLocation(String location) { this.location = location; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}

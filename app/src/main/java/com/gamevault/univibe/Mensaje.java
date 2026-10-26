package com.gamevault.univibe;

public class Mensaje {
    private String messageId;
    private String senderUid;
    private String receiverUid;
    private String text;
    private String imageUrl;
    private long timestamp;
    private String type; // TEXT, IMAGE

    public Mensaje() {
        // Required empty constructor for Firebase DataSnapshot.getValue(Mensaje.class)
    }

    public Mensaje(String messageId, String senderUid, String receiverUid, String text, String imageUrl, long timestamp, String type) {
        this.messageId = messageId;
        this.senderUid = senderUid;
        this.receiverUid = receiverUid;
        this.text = text;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getMessageId() { return messageId != null ? messageId : ""; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderUid() { return senderUid != null ? senderUid : ""; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getReceiverUid() { return receiverUid != null ? receiverUid : ""; }
    public void setReceiverUid(String receiverUid) { this.receiverUid = receiverUid; }

    public String getText() { return text != null ? text : ""; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type != null ? type : "TEXT"; }
    public void setType(String type) { this.type = type; }
}

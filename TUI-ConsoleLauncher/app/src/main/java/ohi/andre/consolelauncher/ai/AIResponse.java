package ohi.andre.consolelauncher.ai;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response model for MiniMax API chat completions.
 * This class represents the structure of the API response.
 */
public class AIResponse {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("object")
    private String object;
    
    @SerializedName("created")
    private long created;
    
    @SerializedName("model")
    private String model;
    
    @SerializedName("choices")
    private List<Choice> choices;
    
    @SerializedName("usage")
    private Usage usage;
    
    public AIResponse() {}
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public long getCreated() {
        return created;
    }
    
    public void setCreated(long created) {
        this.created = created;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Choice> getChoices() {
        return choices;
    }
    
    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
    
    public Usage getUsage() {
        return usage;
    }
    
    public void setUsage(Usage usage) {
        this.usage = usage;
    }
    
    /**
     * Gets the content of the first choice.
     * 
     * @return Response text, or empty string if no choices
     */
    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        return choices.get(0).getMessage().getContent();
    }
    
    /**
     * Gets the full response as a formatted string.
     * 
     * @return Formatted response string
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Model: ").append(model).append("\n");
        sb.append("Created: ").append(created).append("\n");
        
        if (usage != null) {
            sb.append("Tokens - Prompt: ").append(usage.getPromptTokens())
              .append(", Completion: ").append(usage.getCompletionTokens())
              .append(", Total: ").append(usage.getTotalTokens()).append("\n");
        }
        
        if (choices != null) {
            for (int i = 0; i < choices.size(); i++) {
                sb.append("\nChoice ").append(i + 1).append(":\n");
                sb.append(choices.get(i).getMessage().getContent());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Represents a single choice in the response.
     */
    public static class Choice {
        
        @SerializedName("index")
        private int index;
        
        @SerializedName("message")
        private Message message;
        
        @SerializedName("finish_reason")
        private String finishReason;
        
        @SerializedName("delta")
        private Message delta;
        
        public Choice() {}
        
        public int getIndex() {
            return index;
        }
        
        public void setIndex(int index) {
            this.index = index;
        }
        
        public Message getMessage() {
            return message;
        }
        
        public void setMessage(Message message) {
            this.message = message;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
        
        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
        
        public Message getDelta() {
            return delta;
        }
        
        public void setDelta(Message delta) {
            this.delta = delta;
        }
    }
    
    /**
     * Represents a chat message.
     */
    public static class Message {
        
        @SerializedName("role")
        private String role;
        
        @SerializedName("content")
        private String content;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
    
    /**
     * Represents token usage statistics.
     */
    public static class Usage {
        
        @SerializedName("prompt_tokens")
        private int promptTokens;
        
        @SerializedName("completion_tokens")
        private int completionTokens;
        
        @SerializedName("total_tokens")
        private int totalTokens;
        
        public Usage() {}
        
        public int getPromptTokens() {
            return promptTokens;
        }
        
        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }
        
        public int getCompletionTokens() {
            return completionTokens;
        }
        
        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }
        
        public int getTotalTokens() {
            return totalTokens;
        }
        
        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}

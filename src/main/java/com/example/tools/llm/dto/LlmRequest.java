// package com.example.tools.llm.dto;

// import java.util.List;

// public class LlmRequest {

//     private String model;
//     private List<Message> messages;
//     private String response_format; // e.g. "json" for some providers (optional)

//     public LlmRequest() {
//     }

//     public LlmRequest(String model, List<Message> messages) {
//         this.model = model;
//         this.messages = messages;
//     }

//     public String getModel() {
//         return model;
//     }

//     public void setModel(String model) {
//         this.model = model;
//     }

//     public List<Message> getMessages() {
//         return messages;
//     }

//     public void setMessages(List<Message> messages) {
//         this.messages = messages;
//     }

//     public String getResponse_format() {
//         return response_format;
//     }

//     public void setResponse_format(String response_format) {
//         this.response_format = response_format;
//     }

//     public static class Message {
//         private String role;
//         private String content;

//         public Message() {
//         }

//         public Message(String role, String content) {
//             this.role = role;
//             this.content = content;
//         }

//         public String getRole() {
//             return role;
//         }

//         public void setRole(String role) {
//             this.role = role;
//         }

//         public String getContent() {
//             return content;
//         }

//         public void setContent(String content) {
//             this.content = content;
//         }
//     }
// }

package com.example.tools.llm.dto;

import java.util.List;

public class LlmRequest {

    private String model;
    private List<Message> messages;
    // Optional: some providers accept response_format (can be null)
    private Object response_format;
    // 🔴 NEW: limit response size so we don’t hit token/credit limits
    private Integer max_tokens;

    public LlmRequest() {
    }

    public LlmRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Object getResponse_format() {
        return response_format;
    }

    public void setResponse_format(Object response_format) {
        this.response_format = response_format;
    }

    // 🔴 NEW
    public Integer getMax_tokens() {
        return max_tokens;
    }

    // 🔴 NEW
    public void setMax_tokens(Integer max_tokens) {
        this.max_tokens = max_tokens;
    }

    public static class Message {
        private String role;
        private String content;

        public Message() {
        }

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
}

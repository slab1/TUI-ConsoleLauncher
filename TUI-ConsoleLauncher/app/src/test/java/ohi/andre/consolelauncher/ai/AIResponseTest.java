package ohi.andre.consolelauncher.ai;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for AI response model classes.
 */
public class AIResponseTest {
    
    @Test
    public void testAIResponseCreation() {
        AIResponse response = new AIResponse();
        
        assertNotNull(response);
        assertNull(response.getId());
        assertNull(response.getObject());
        assertEquals(0, response.getCreated());
        assertNull(response.getModel());
        assertNull(response.getChoices());
        assertNull(response.getUsage());
    }
    
    @Test
    public void testAIResponseSetters() {
        AIResponse response = new AIResponse();
        
        response.setId("test-id");
        response.setObject("chat.completion");
        response.setCreated(1234567890L);
        response.setModel("abab5.5-chat");
        
        assertEquals("test-id", response.getId());
        assertEquals("chat.completion", response.getObject());
        assertEquals(1234567890L, response.getCreated());
        assertEquals("abab5.5-chat", response.getModel());
    }
    
    @Test
    public void testAIResponseGetContentEmpty() {
        AIResponse response = new AIResponse();
        
        assertEquals("", response.getContent());
    }
    
    @Test
    public void testAIResponseGetContentWithChoices() {
        AIResponse response = new AIResponse();
        
        List<AIResponse.Choice> choices = new ArrayList<>();
        AIResponse.Choice choice = new AIResponse.Choice();
        AIResponse.Message message = new AIResponse.Message();
        message.setContent("Hello, I am an AI assistant.");
        choice.setMessage(message);
        choices.add(choice);
        
        response.setChoices(choices);
        
        assertEquals("Hello, I am an AI assistant.", response.getContent());
    }
    
    @Test
    public void testAIResponseToFormattedString() {
        AIResponse response = new AIResponse();
        response.setModel("test-model");
        response.setCreated(1234567890L);
        
        AIResponse.Usage usage = new AIResponse.Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);
        response.setUsage(usage);
        
        List<AIResponse.Choice> choices = new ArrayList<>();
        AIResponse.Choice choice = new AIResponse.Choice();
        choice.setIndex(0);
        AIResponse.Message message = new AIResponse.Message();
        message.setRole("assistant");
        message.setContent("Test response");
        choice.setMessage(message);
        choice.setFinishReason("stop");
        choices.add(choice);
        response.setChoices(choices);
        
        String formatted = response.toFormattedString();
        
        assertNotNull(formatted);
        assertTrue(formatted.contains("Model: test-model"));
        assertTrue(formatted.contains("Test response"));
    }
    
    @Test
    public void testMessageCreation() {
        AIResponse.Message message = new AIResponse.Message();
        
        assertNull(message.getRole());
        assertNull(message.getContent());
        
        message.setRole("user");
        message.setContent("Hello");
        
        assertEquals("user", message.getRole());
        assertEquals("Hello", message.getContent());
    }
    
    @Test
    public void testMessageConstructor() {
        AIResponse.Message message = new AIResponse.Message("assistant", "I can help you.");
        
        assertEquals("assistant", message.getRole());
        assertEquals("I can help you.", message.getContent());
    }
    
    @Test
    public void testChoiceCreation() {
        AIResponse.Choice choice = new AIResponse.Choice();
        
        assertEquals(0, choice.getIndex());
        assertNull(choice.getMessage());
        assertNull(choice.getFinishReason());
    }
    
    @Test
    public void testChoiceSetters() {
        AIResponse.Choice choice = new AIResponse.Choice();
        
        choice.setIndex(1);
        choice.setFinishReason("stop");
        
        assertEquals(1, choice.getIndex());
        assertEquals("stop", choice.getFinishReason());
    }
    
    @Test
    public void testUsageCreation() {
        AIResponse.Usage usage = new AIResponse.Usage();
        
        assertEquals(0, usage.getPromptTokens());
        assertEquals(0, usage.getCompletionTokens());
        assertEquals(0, usage.getTotalTokens());
    }
    
    @Test
    public void testUsageSetters() {
        AIResponse.Usage usage = new AIResponse.Usage();
        
        usage.setPromptTokens(100);
        usage.setCompletionTokens(200);
        usage.setTotalTokens(300);
        
        assertEquals(100, usage.getPromptTokens());
        assertEquals(200, usage.getCompletionTokens());
        assertEquals(300, usage.getTotalTokens());
    }
    
    @Test
    public void testMiniMaxServiceMessageCreation() {
        MiniMaxService.Message message = new MiniMaxService.Message();
        
        assertNull(message.getRole());
        assertNull(message.getContent());
        
        message.setRole("user");
        message.setContent("Test message");
        
        assertEquals("user", message.getRole());
        assertEquals("Test message", message.getContent());
    }
    
    @Test
    public void testMiniMaxServiceMessageConstructor() {
        MiniMaxService.Message message = new MiniMaxService.Message("system", "You are helpful.");
        
        assertEquals("system", message.getRole());
        assertEquals("You are helpful.", message.getContent());
    }
    
    @Test
    public void testMiniMaxServiceChatRequest() {
        MiniMaxService.ChatRequest request = new MiniMaxService.ChatRequest();
        
        assertNull(request.getModel());
        assertNull(request.getMessages());
        assertEquals(0, request.getTemperature(), 0.001);
        assertEquals(0, request.getMaxTokens());
        assertFalse(request.isStream());
        
        request.setModel("test-model");
        request.setTemperature(0.7f);
        request.setMaxTokens(1000);
        request.setStream(true);
        
        assertEquals("test-model", request.getModel());
        assertEquals(0.7f, request.getTemperature(), 0.001);
        assertEquals(1000, request.getMaxTokens());
        assertTrue(request.isStream());
    }
    
    @Test
    public void testAIException() {
        AIException exception = new AIException("Test error");
        
        assertEquals("Test error", exception.getMessage());
        
        Throwable cause = new RuntimeException("Cause");
        AIException exceptionWithCause = new AIException("Error with cause", cause);
        
        assertEquals("Error with cause", exceptionWithCause.getMessage());
        assertEquals(cause, exceptionWithCause.getCause());
    }
    
    @Test
    public void testStreamingCallback() {
        // Test the streaming callback interface
        MiniMaxService.StreamingCallback callback = new MiniMaxService.StreamingCallback() {
            private String fullContent = "";
            
            @Override
            public void onChunk(String chunk, String currentFullContent) {
                this.fullContent = currentFullContent;
            }
            
            @Override
            public void onComplete(String fullContent) {
                this.fullContent = fullContent;
            }
            
            @Override
            public void onError(Exception e) {
                // Handle error
            }
        };
        
        // Test callback methods
        callback.onChunk("Hello", "Hello");
        callback.onComplete("Hello world");
        callback.onError(new Exception("Test"));
        
        assertTrue(true); // No exceptions means success
    }
}

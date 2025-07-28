# Buffer Extension Backend

A Spring Boot backend service that analyzes web content and generates creative, platform-specific social media content ideas for small businesses using OpenAI's API.

## 🚀 Features

- **Content Analysis**: Analyze web page content and extract meaningful insights
- **AI-Powered Ideas**: Generate 3-5 creative content ideas per social media platform
- **Multi-Platform Support**: Instagram, X (Twitter), LinkedIn with platform-specific recommendations
- **Session Management**: Store and track analysis sessions with comprehensive monitoring
- **Robust Error Handling**: Clear failure messages with retry logic for API calls
- **RESTful API**: Clean endpoints for content analysis and monitoring

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **OpenAI API Key** (with GPT-4 access recommended)

## ⚙️ Local Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Extension-BE
```

### 2. Create Application Properties
Since `application.properties` is in `.gitignore`, you need to create it locally:

#### Option A: Copy from Template (Recommended)
```bash
# Copy the template file
cp src/main/resources/application.properties.template src/main/resources/application.properties
```

Then edit `src/main/resources/application.properties` and replace `your-openai-api-key-here` with your actual OpenAI API key.

#### Option B: Create from Scratch
```bash
# Create the application.properties file
touch src/main/resources/application.properties
```

Add the following content to `src/main/resources/application.properties`:
```properties
# Server Configuration
server.port=${PORT:8080}

# OpenAI API Configuration
openai.api.key=your-openai-api-key-here

# Spring Profile
spring.profiles.active=prod
```

**⚠️ Important**: Replace `your-openai-api-key-here` with your actual OpenAI API key from [OpenAI Platform](https://platform.openai.com/api-keys).

### 3. Build the Application
```bash
mvn clean compile
```

### 4. Run the Application

#### Option A: Using Maven (Development)
```bash
mvn spring-boot:run
```

#### Option B: Using JAR (Production-like)
```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/buffer-extension-backend-1.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## 📡 API Endpoints

### Content Analysis
- **POST** `/api/context` - Analyze web content and generate ideas

**Request Example:**
```json
{
  "title": "The Impact of Customer Reviews on Small Businesses",
  "fullText": "In today's interconnected world, customer reviews have become fundamental...",
  "description": "Article about customer review impact",
  "url": "https://example.com/article",
  "headings": ["Trust and Credibility", "SEO Benefits"],
  "channels": ["Instagram", "X", "LinkedIn"]
}
```

**Success Response:**
```json
{
  "status": "SUCCESS",
  "summary": "Analysis of customer review impact article",
  "chatID": "session-uuid",
  "channels": {
    "Instagram": [
      {
        "ideaId": "idea_123",
        "description": "Create an infographic featuring statistics...",
        "rationale": "Infographics perform well on Instagram...",
        "pros": ["Visually appealing", "Shareable"],
        "cons": ["May not provide in-depth insights"]
      }
    ]
  }
}
```

**Failure Response:**
```json
{
  "status": "FAILURE",
  "summary": "No content provided for analysis - please ensure the page content is being captured properly...",
  "chatID": "",
  "channels": {}
}
```

### Monitoring Endpoints
- **GET** `/api/monitor/health` - Health check
- **GET** `/api/monitor/database` - Database statistics
- **GET** `/api/monitor/sessions` - All analysis sessions
- **GET** `/api/monitor/session/{sessionId}` - Specific session details

## 🏗️ Architecture

### Core Components

1. **ContextController**: Handles incoming content analysis requests
2. **ContextAnalysisService**: Orchestrates the analysis workflow
3. **OpenAIService**: Manages OpenAI API communication with retry logic
4. **ContentIdeaDatabase**: In-memory storage for sessions and results
5. **MonitoringController**: Provides system health and analytics

### Data Flow

```
Browser Extension → ContextController → ContextAnalysisService → OpenAIService → OpenAI API
                                    ↓
                               ContentIdeaDatabase ← Parse & Store Results
```

### Retry & Resilience

- **Exponential Backoff**: 3 retries with 1-5 second delays
- **Rate Limit Handling**: Automatic retry on 429 errors
- **Error Recovery**: Graceful failure with descriptive messages
- **No Fallback Content**: Honest feedback instead of generic responses

## 🔧 Configuration

### Default Channels
- Instagram
- X (formerly Twitter)  
- LinkedIn

### Content Analysis
- **Minimum Content**: Meaningful text required (no empty/minimal content)
- **AI Model**: Uses OpenAI's structured output for consistent JSON responses
- **Session Limit**: Maximum 50 sessions (LRU eviction)
- **Ideas Per Channel**: 3-5 platform-specific content ideas

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8080/api/monitor/health
```

### Database Stats
```bash
curl http://localhost:8080/api/monitor/database
```

### View All Sessions
```bash
curl http://localhost:8080/api/monitor/sessions
```

## 🧪 Testing

### Test Content Analysis
```bash
curl -X POST http://localhost:8080/api/context \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Article",
    "fullText": "This is a comprehensive article about social media marketing strategies for small businesses. It covers various platforms and their unique advantages...",
    "description": "Marketing strategies guide",
    "url": "https://example.com/marketing-guide"
  }'
```

### Test Error Handling
```bash
curl -X POST http://localhost:8080/api/context \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Empty Test",
    "fullText": "",
    "description": "Testing empty content",
    "url": "https://example.com/empty"
  }'
```

## 🚨 Troubleshooting

### Common Issues

1. **"OpenAI API Error 401"**
   - Check your API key in `application.properties`
   - Ensure the key has proper permissions

2. **"No content provided for analysis"**
   - Verify the `fullText` field is not empty
   - Check browser extension is capturing content properly

3. **"AI analysis failed: insufficient content"**
   - Content must be substantial enough for meaningful analysis
   - Ensure the content has depth and specific information

4. **Connection timeouts**
   - Check internet connectivity
   - Verify OpenAI API status

## 📝 Development Notes

- **No Chat Functionality**: All chat-related features have been removed
- **Context-Only**: Focus is solely on content analysis and idea generation
- **Stateless**: Each request is independent (except for session storage)
- **Production Ready**: Includes proper error handling, logging, and monitoring

## 🔄 Recent Changes

- ✅ Removed deprecated chat functionality
- ✅ Eliminated fallback response generation
- ✅ Improved error handling with clear failure messages
- ✅ Added summary field to Context entity
- ✅ Enhanced monitoring endpoints
- ✅ Simplified codebase architecture

---

**Version**: 3.0.0-content-ideas  
**Last Updated**: July 2025
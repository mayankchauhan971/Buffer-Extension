# Buffer Extension Backend

A simple web service that turns website content into social media post ideas using AI.

## What it does

Send web page content to this service and get back creative social media post ideas for Instagram, X (Twitter), and LinkedIn. Perfect for content creators and marketers who need fresh ideas quickly.

## Requirements

- Java 17 or newer
- Maven 3.6+
- OpenAI API key

## Quick Start

1. **Get the code**
   ```bash
   git clone <repository-url>
   cd Extension-BE
   ```

2. **Add your OpenAI API key**
   
   Edit `src/main/resources/application.properties` and add your API key:
   ```properties
   server.port=${PORT:8080}
   openai.api.key=your-actual-api-key-here
   spring.profiles.active=prod
   ```
   
   Get your API key at [OpenAI Platform](https://platform.openai.com/api-keys).

3. **Start the service**
   ```bash
   mvn spring-boot:run
   ```
   
   The service starts at `http://localhost:8080`

## How to use it

Send a POST request to `/api/context` with your content:

```bash
curl -X POST http://localhost:8080/api/context \
  -H "Content-Type: application/json" \
  -d '{
    "title": "10 Marketing Tips for Small Business",
    "fullText": "Small businesses face unique challenges in marketing. Here are proven strategies that work...",
    "url": "https://example.com/article"
  }'
```

You'll get back social media ideas for each platform:

```json
{
  "status": "SUCCESS",
  "summary": "Marketing tips analyzed successfully",
  "chatID": "session-12345",
  "channels": {
    "Instagram": [
      {
        "description": "Create a carousel post with each tip as a slide...",
        "rationale": "Carousels get high engagement on Instagram",
        "pros": ["Visual appeal", "Easy to share"],
        "cons": ["Takes time to design"]
      }
    ]
  }
}
```

## Other useful endpoints

- `GET /api/monitor/health` - Check if service is running
- `GET /api/monitor/sessions` - See all analysis sessions

## Production deployment

1. **Build the application**
   ```bash
   mvn clean package
   ```

2. **Run with Docker**
   ```bash
   docker build -t buffer-extension .
   docker run -p 8080:8080 buffer-extension
   ```

## Troubleshooting

**"OpenAI API Error 401"** - Double-check your API key is correct

**"No content provided"** - Make sure the `fullText` field isn't empty

**Service won't start** - Verify Java 17+ is installed: `java -version`
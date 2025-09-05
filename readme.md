## TL;DR
Content ideation is hard because creativity isn’t constant—it surges and stalls. What if ideation were effortless—a copilot that, with one click on any page or article, generates multiple socialMediaChannel-specific ideas (not finished posts) with clear rationale, pros and cons, and the option to expand any contentIdea into a polished post—each tailored to brand voice and goals and delivered straight into Buffer for scheduling and collaboration.

Welcome to a mini-project crafted to help me be considered for an interview at Buffer:  **a Content Ideation Copilot Chrome extension.**

**Screenshots**

<img width="376" height="735" alt="flowStart" src="https://github.com/user-attachments/assets/cc0b7f9a-cb7b-472d-8aca-f754135a4551" />
<img width="375" height="730" alt="analyzeContent" src="https://github.com/user-attachments/assets/1cd1c04f-15df-4377-922b-69aa606f83ee" />
<img width="439" height="735" alt="flowScreen" src="https://github.com/user-attachments/assets/9d8c74a5-3919-497f-bea8-63274d74cbaf" />
<img width="434" height="738" alt="ideasGenerated" src="https://github.com/user-attachments/assets/98f3eb50-5c44-4f02-84cd-cfe879ab92fe" />
<img width="699" height="942" alt="bufferExport" src="https://github.com/user-attachments/assets/0259365c-d1b4-4384-b89d-f2a58bda9b91" />


---

### Demo Video
Demo Link -
https://www.loom.com/share/5a9098e4ccc04ff693d92709ebf89151?sid=3521e6f5-56b4-4889-af70-69139a8400a5

---

## Problem Statement

### **Who are the current users?**
I think current Buffer users can be grouped in 2 buckets:

- **SMM** – for whom managing social is their primary job. They have to think about multiple channels and need to have ideas in the content pipeline with strict brand adherence.
- **SMB owners / Solopreneurs** – social is their secondary job. Pain point is limited time ([link](https://buffer.com/resources/time-saving-tools/)), frequent context switching, and limited focus.

---

### **How does the current content creation process look like?**

- **Push (inspiration-driven):**  
  See a reel/post/article while browsing → capture the reference and a rough note → save as an Idea for later.

- **Pull (planning-driven):**  
  Sit down for weekly/monthly planning → research topics and references → outline concepts → draft → refine → iterate → queue.

---

### **What problems do they face?**
- Ideation is cognitively heavy; on busy or low-energy days, turning references into on-brand concepts drains energy that would be better spent on engagement or collaboration.
- Generic AI prompts produce bland, off-brand concepts. You often need to provide a lot of context and then need to manage all your contexts (aka rely on tools like Spiral, etc.). More point solutions → more context switching.
- Different tone, format, and structure work on different channels. So early ideas are captured socialMediaChannel-agnostic and later tailored to each socialMediaChannel—adding extra effort from Idea to Draft.

---

We aim to assist SMM/SMB owners to save time in content creation by providing them with a **creative copilot** which takes what’s on their screen (any article/webpage/text content) and, based on your brand's context, provides you **content ideas specific to socialMediaChannel in your brand voice**.

---

## How does this work? [Product Side]

- User would authenticate with Buffer, using OAuth – (*Not Implemented Yet but can be done with access to Buffer's API*)
- Open the extension and click **Brainstorm with AI**
- Select the socialMediaChannel they are interested in to get multiple ideas, look at the pros and cons
- Save the Idea, prompt it further in Buffer's editor
- Can use Buffer's AI assistant in the editor to prompt it further

---

## How does this work? [Technical Side]
<img width="811" height="505" alt="Screenshot 2025-09-01 at 7 43 52 PM" src="https://github.com/user-attachments/assets/22b8de3f-b3de-4d1b-bf4b-82768e362273" />


API: Uses OpenAI Responses API (/responses) via Spring WebClient. Model, temperature, base URL, and defaults are centralized in com.buffer.config.AIConstants.

Structured output: We request JSON via text.format with type=json_schema, name=content_ideas_schema, and strict=true. The schema enforces:
- status and summary
- channels object with only requested channels
- Each channel has an array of up to 3 ideas (idea, rationale, pros, cons) per AIConstants.IDEA_MAX_ITEMS.

Retries: Exponential backoff with Reactor Retry.backoff, maxAttempts=3, initial delay 1s, max backoff 5s. Retries on 429 (rate limit), 500+ (server errors), and 408 (request timeout).

Storage: Repository layer follows repository pattern, currently using an in-mem DB which can be swapped for any DB, cache, etc.

Monitoring: The Monitoring controller interacts and exposes storage methods, mainly for debugging purposes, currently.

**Sequence Diagram**

<img width="697" height="1194" alt="Screenshot 2025-09-02 at 1 11 57 PM" src="https://github.com/user-attachments/assets/85282e0e-85fd-4973-abe1-e7078492048f" />


I'm a backend engineer with limited frontend knowledge and used LLM for coding UI. Backend is built in Java, language I'm most comfortable in. Authentication flow is not implemented since Buffer no longer supports the creation of new developer apps. [https://buffer.com/developers/api/oauth]

---

## Why did I do this?

I know Buffer gets tons of great applications so had to get creative to stand out :)

Video on my rationale and small introduction - https://www.loom.com/share/705df813dd074a19a6bd716e6f80aa47?sid=8b3b6add-cf2d-4df8-b4ca-8ceea3af4567
- I love the idea of remote work, radical transparency, and gratefulness (something I'm trying to inculcate in daily life) and would love to get a chance to interview for a backend engineer role at Buffer.
- I have worked in the social field (managed Sprinklr's publishing product, transitioned from product management to engineering), have created content on LinkedIn (grew it to 8K followers) and Instagram (to market my startups), and love building things. I think I might be a good fit for engineering role at Buffer

---

### Learn more about me
- **Website:** [https://mayank.foo/](mayank.foo)
- **LinkedIn:** [https://www.linkedin.com/in/mayankchauhan971/](https://www.linkedin.com/in/mayankchauhan971/)

---

This is V0 — if things work out, maybe I can build this at Buffer :)  

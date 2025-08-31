# Content Ideation Copilot – Chrome Extension

## TL;DR
Content ideation is hard because creativity isn’t constant—it surges and stalls.

**What if ideation were effortless—a copilot that, with one click on any page or article, generates multiple socialMediaChannel-specific ideas (not finished posts) with clear rationale, pros and cons, and the option to expand any contentIdea into a polished post—each tailored to brand voice and goals and delivered straight into Buffer for scheduling and collaboration.**

Welcome to a mini-project crafted to help me be considered for an interview at Buffer:  
**a Content Ideation Copilot Chrome extension.**

---

### 🎥 Demo Video
Demo Link -
https://www.loom.com/share/705df813dd074a19a6bd716e6f80aa47?sid=63f62595-4260-4443-b484-338e96169017

---

## Problem Statement

### **Who are the current users?**
I think current Buffer users can be grouped in 2 buckets:

- **SMM** – for whom managing social is their primary job. They have to think about multiple channels and need to have ideas in the content pipeline with strict brand adherence.
- **SMB owners / Solopreneurs** – social is their secondary job. Pain point is limited time ([link](https://buffer.com/resources/time-saving-tools/)), frequent context switching, and limited focus.

---

### **How does the current process look like?**

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
![Architecture Screenshot](link-to-architecture-screenshot)

Retry Logic - Retrying 3 times with exponential backoff on particular error codes like Rate limit, Server errors and request timeouts.

I'm a backend engineer with limited frontend knowledge and used LLM for coding UI. Backend is built in Java, language I'm most comfortable in.

---

## Why did I do this?
Video on my rationale and small introduction - https://www.loom.com/share/705df813dd074a19a6bd716e6f80aa47?sid=8b3b6add-cf2d-4df8-b4ca-8ceea3af4567
- I love the contentIdea of remote work, radical transparency, and gratefulness (something I'm trying to inculcate in daily life) and would love to get a chance to interview for a backend engineer role at Buffer.
- I have worked in the social field (managed Sprinklr's publishing product), have created content on LinkedIn (grew it to 8K followers) and Instagram (to market my startups), and love building things. I think I might be a good fit for engineering role at Buffer

---

### Learn more about me
- **Website:** [https://mayank.foo/](mayank.foo)
- **LinkedIn:** [https://www.linkedin.com/in/mayankchauhan971/](https://www.linkedin.com/in/mayankchauhan971/)

---

This is V0 — if things work fine, maybe I can build this at Buffer :)  

# Full Live Deployment Guide

Deploying a modern application involves three distinct layers: Database, Backend, and Frontend. 

## Will it update automatically?
**YES!** If you use **GitHub** as your codebase bridge, all modern hosting providers offer **Continuous Deployment (CD)**. 
Once you link the platforms discussed below, anytime you make a fix in VS Code and "Push" that code to GitHub, the hosting platforms will instantly detect the change, temporarily recompile your Java, and deploy your live website completely automatically without you touching a terminal!

---

## Step 1: Upload to GitHub (Crucial)
Every free modern hosting pipeline requires you to bring your code via GitHub.
1. Create a free account at [GitHub](https://github.com/).
2. Create a new repository (e.g., `ExpenseIQ`).
3. Using VS Code's built-in Source Control tab (or terminal), commit your entire project folder and push it up to that GitHub repository.

## Step 2: The Database (Supabase)
Currently, you use an H2 database that deletes itself when the server stops. We must make it permanent.
1. Go to [Supabase.com](https://supabase.com) and create a "New Project". 
2. Wait a few minutes for the servers to provision.
3. Go to **Project Settings -> Database** and scroll down to the **JDBC Connection String**.
4. In your code (`backend/src/main/resources/application.properties`), delete the `spring.datasource.url=jdbc:h2:mem` line, and replace it with the new live Supabase URL! Also remember to adjust `spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect`.

## Step 3: The Backend (Render)
We will host the heavy Spring Boot Java application here for free.
1. Sign up for [Render.com](https://render.com) using your GitHub account.
2. Click **New +** and select **Web Service**.
3. Select your `ExpenseIQ` GitHub repository.
4. Fill in these specific exact settings so Render knows how to deal with Java:
   - **Root Directory:** `backend`
   - **Environment:** `Java`
   - **Build Command:** `./mvnw clean install -DskipTests`
   - **Start Command:** `java -jar target/*.jar`
5. Click Deploy. It will give you a live URL like `https://expenseiq-backend.onrender.com`.

## Step 4: The Frontend (Vercel)
Finally, we host the beautiful UI. *Before you do this, you must open your `frontend/auth.html` and change `const API_BASE = 'http://localhost:8080/api/auth';` to use your new Render URL!*
1. Sign up for [Vercel.com](https://vercel.com) using your GitHub account.
2. Click **Add New Project** and select your GitHub repository.
3. Under settings:
   - Set the **Root Directory** to point to `frontend`.
   - Set Framework Preset to **Other** (since we use raw vanilla HTML).
4. Click **Deploy**. Vercel takes about 10 seconds and will instantly spit out a beautiful, free SSL-secured domain for your website (e.g., `expenseiq.vercel.app`)!

> [!WARNING] The Final Security Check (CORS)
> Because of browser security, applications cannot talk to domains they don't trust. 
> Once Vercel gives you your live frontend URL (e.g., `https://expenseiq.vercel.app`), you MUST go into your backend's `application.properties` in VS Code and change `app.cors.allowed-origins` to include that exact Vercel URL. Push the change to GitHub, and Render will automatically restart your backend. You are now live to the world!

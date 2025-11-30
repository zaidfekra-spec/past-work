Minecraft Plugin Portfolio

This repository showcases several past projects I developed using Java and the Spigot/Bukkit API.  
Each plugin demonstrates different skills: event handling, commands, concurrency, database integration, and NMS usage.

---

🔥 Plugins Included

KillTrackerPlugin
- Tracks player kills via `PlayerDeathEvent`
- `/kills` command with subcommands (`top`, `reset`, `save`)
- Async saving to file/MySQL
- Demonstrates concurrency, dependency injection, and efficient code

WelcomeMessagePlugin
- Sends styled join and first‑join messages
- Configurable messages with placeholders (`{player}`, `{world}`, `{time}`)
- `/welcome reload|set|worlds` command for live config management
- Shows event handling, permissions, and dynamic placeholders

DatabaseExamplePlugin
- Async integration with MySQL
- Commands: `/db set|get|synconline`
- Demonstrates `CompletableFuture`, caching, and syncing vanilla statistics
- Highlights database knowledge and efficient async I/O

NMSExamplePlugin
- Demonstrates safe NMS usage (1.19_R3)
- Spawns client‑side glowing ArmorStands with custom names
- Shows moderate NMS knowledge and version abstraction

---

🛠️ Skills Demonstrated
- 2+ years of Java experience
- 2+ years with Spigot & Bukkit API
- Moderate knowledge of NMS
- Database integration (MySQL, MongoDB, Redis)
- Strong understanding of concurrency & dependency injection
- Efficient, clean code design
- Git & Maven project discipline

---

🚀 How to Run
1. Clone this repository.
2. Build each plugin with Maven (`mvn package`) or drop the `.java` file into a Spigot project.
3. Place the resulting JAR in your server’s `plugins/` folder.
4. Adjust configs (e.g., database credentials) as needed.



 📄 License
This code is provided for demonstration purposes.  
Feel free to explore and learn from it, but please credit if reused.

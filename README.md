# Spring-AxiomBank-BulletProof-Engine 🛡️
> **High-load banking engine with zero-compromise security (If you find congrats, register on bug bounty, earn money).**

![Spring Boot](https://img.shields.io/badge/Spring--Boot-3.x-brightgreen?style=flat-square)
![Spring Security](https://img.shields.io/badge/Spring--Security-7.x-blue?style=flat-square)
![Auth](https://img.shields.io/badge/Auth-JWT--over--Cookies-red?style=flat-square)

## ⚡ The Architecture
This is a hardened financial backend core. While most implementations leave the door cracked open with local storage tokens, I use a fortress-first approach. 

Built on the **Spring Security 7.0** baseline, this engine is optimized for high-throughput banking transactions where latency is low but the threat model is high.

### 🔐 Security Implementation
* **JWT-over-Cookies**: Tokens are encapsulated in `HttpOnly`, `Secure`, and `SameSite=Lax` cookies. JavaScript cannot touch them. XSS is dead on arrival.
* **Spring Security 7 Baseline**: Utilizing the latest non-blocking filter chain for maximum performance under heavy load.
* **Stateless Persistence**: Zero session overhead. The server remembers nothing; the crypto explains everything.
* **Zero-Knowledge Config**: No plaintext secrets.
* **Race condition protection**: I use softlock on almost all methods inside transactions so you cant make infinite money glitch
* **Redis**: Brute-force and spam protection



## 🛠️ Tech Stack
* **Java 21** (LTS)
* **Spring Boot 3.x**
* **Spring Security 7.x** (Early Adopter Baseline)
* **PostgreSQL** (with optimized indexing for high-load)
* **Maven**
* **Redis**



....Docker is coming soon maybe

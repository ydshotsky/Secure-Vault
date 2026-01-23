Secure Vault

The Problem

Cryptographic operations (Argon2, BCrypt, Scrypt) are designed to be CPU-intensive to thwart brute-force attacks. However, in a web environment,a sudden 
burst of authentication requests can saturate the CPU, leading to cascading latency—where even simple, non-crypto requests (like health checks or static
assets) take seconds to load because the CPU scheduler is overwhelmed.

The Solution

Secure Vault implements an "Admission Control" pattern. By using a global Semaphore as a CPU budget and a dedicated ExecutorService, the system explicitly
bounds the number of concurrent cryptographic operations. This ensures that the system remains responsive, rejecting excess load early rather than 
degrading for everyone.


🏗 System Architecture

The application separates I/O-bound tasks from CPU-bound tasks:

I/O Layer: Leverages Java Virtual Threads to handle thousands of concurrent web connections efficiently.

Admission Control: A Global Semaphore acts as a "Gatekeeper," limiting active crypto-work to a specific CPU budget (e.g., Available Processors - 1).

Execution Layer: A bounded ExecutorService processes the actual hashing/derivation logic.


🛠 Tech Stack

Backend: Java 21, Spring Boot 3.5.6x

Concurrency & Performance:
    
    ExecutorService for isolating CPU-bound work

    Global Semaphore as a CPU budget
    
    Virtual Threads enabled for scalable I/O

Frontend: Thymeleaf, CSS, JavaScript

📊 Performance Evidence: The "Burst Test"

Test scenario

  Threads: 20
  
  Ramp-up: 0 (simultaneous burst)
  
  Loop count: 1

Target: CPU-heavy authentication path(eg :signup,unlock,reveal)

This test compares system behavior with and without CPU admission control under identical conditions.

           _________________________________________________________________________________
           Metric         |With Admission Control (Safe) |Without Control (Oversubscribed)  |
           _______________|______________________________|__________________________________|

           CPU Plateau    |~25% (Stable)                 |~62% (Spiked)                     |

           p95 Latency    |798 ms                        |1351 ms                           |

           Min Latency    |166 ms                        |914 ms                            |

           System Health  |Responsive, 17 rejected early |Unresponsive, all requests delayed|
           _______________|______________________________|__________________________________|

Key Takeaway

Without admission control, heavy cryptographic tasks flood the CPU scheduler, causing even lightweight GET requests to suffer high latency due to 
CPU-level head-of-line blocking.
With a CPU budget, overload is handled honestly:

  CPU stays bounded
  
  Latency remains predictable
  
  Failure is explicit instead of silent.

It is better to fail fast for some users than to provide a broken experience for all.

## Project Status

This project is complete as a performance and system-design artifact. It is intended to demonstrate sound concurrency control and overload handling,
not to maximize throughput.

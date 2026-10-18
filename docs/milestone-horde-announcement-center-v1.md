# Horde announcement center v1 — 2026-10-18

- The center-screen countdown title retains its reduced 2.4x scale (vanilla title is 4x) and subtitle below at 2.0x.
- Removed only the extra chat-line copy of each countdown/arrival announcement. The server-side log entry remains for diagnosis.
- Built offline successfully; the combined JAR with the city-entry cover correction is staged in `staging/city-entry-cover-fix-v1-20261018/seven-days-locks-1.0.1.jar`. It is not installed while the user plays.
- After installation, verify a midnight countdown and the arrival notice appear centered with “Prepare your defenses” beneath, without a duplicate chat line. If the centered overlay fails to appear, inspect the client log and packet delivery before relying on chat as a substitute.

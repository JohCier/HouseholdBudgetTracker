# Environment for builds and tests in a Claude Code cloud session: JDK 25 and the Node installed by
# session-start.sh. Sourced by session-start.sh via CLAUDE_ENV_FILE; if that does not reach the Bash tool
# (java -version shows 21), prefix commands with ". .claude/hooks/cloud-env.sh &&".
JDK=/usr/lib/jvm/java-25-openjdk-amd64
NODE_DIR=/opt/node-current/node_modules/node-linux-x64/bin
[ -x "$JDK/bin/java" ] && export JAVA_HOME="$JDK" PATH="$JDK/bin:$PATH"
[ -x "$NODE_DIR/node" ] && export PATH="$NODE_DIR:$PATH"
unset JDK NODE_DIR

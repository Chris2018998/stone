module org.stone {
    requires java.sql;
    requires jakarta.transaction;
    requires java.naming;
    requires java.management;
    requires org.slf4j;
    requires org.javassist;

    exports org.stone.beecp;
    exports org.stone.beecp.jta;
    exports org.stone.beecp.pool;
    exports org.stone.beecp.pool.exception;
    opens org.stone.beecp.pool;

    exports org.stone.beeop;
    exports org.stone.beeop.pool;
    exports org.stone.beeop.pool.exception;
    opens org.stone.beeop.pool;

    exports org.stone.beetp;
    exports org.stone.beetp.pool;
    exports org.stone.beetp.pool.exception;
    opens org.stone.beetp.pool;

    exports org.stone.tools;
    exports org.stone.tools.exception;
    exports org.stone.tools.extension;
    exports org.stone.tools.logger;
}
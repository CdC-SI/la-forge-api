FROM docker-commons.zas.admin.ch/zas/imagebase/application/java:25-openjdk-headless-ubi-2.9.1
COPY target/LaForge.jar /app/
CMD ["java" ,"-jar", "/app/LaForge.jar"]
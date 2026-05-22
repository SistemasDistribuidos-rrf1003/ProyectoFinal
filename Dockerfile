FROM ubuntu:latest
LABEL authors="rorof"

ENTRYPOINT ["top", "-b"]
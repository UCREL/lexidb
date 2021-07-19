#!/bin/bash

DOCKER_BUILDKIT=1 docker build --progress=plain -f Dockerfile -t lexidb:0.0.1 .
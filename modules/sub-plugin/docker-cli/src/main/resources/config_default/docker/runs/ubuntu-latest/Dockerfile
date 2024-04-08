#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

FROM ubuntu:latest
ENV DEBIAN_FRONTEND=noninteractive
ENV LANG C.UTF-8
RUN echo 'deb http://mirrors.aliyun.com/ubuntu/ jammy main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb-src http://mirrors.aliyun.com/ubuntu/ jammy main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb http://mirrors.aliyun.com/ubuntu/ jammy-security main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb-src http://mirrors.aliyun.com/ubuntu/ jammy-security main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb http://mirrors.aliyun.com/ubuntu/ jammy-updates main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb-src http://mirrors.aliyun.com/ubuntu/ jammy-updates main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb http://mirrors.aliyun.com/ubuntu/ jammy-proposed main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb-src http://mirrors.aliyun.com/ubuntu/ jammy-proposed main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb http://mirrors.aliyun.com/ubuntu/ jammy-backports main restricted universe multiverse' >> /etc/apt/sources.list \
    && 'deb-src http://mirrors.aliyun.com/ubuntu/ jammy-backports main restricted universe multiverse' >> /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y curl wget

ENTRYPOINT ["/bin/bash", "/tmp/build.sh"]

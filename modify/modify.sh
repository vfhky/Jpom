#!/bin/bash


if ! test -f "./modify.sh"; then
    echo "请在脚本当前目录执行"
    exit 0
fi

\cp -rf  ./*  ..

rm -rf ../modify.sh

echo "=== end ==="

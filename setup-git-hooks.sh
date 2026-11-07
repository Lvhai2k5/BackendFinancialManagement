#!/bin/sh
# Chạy 1 lần sau khi clone: kích hoạt git hook của dự án (chỉ áp dụng cho repo hiện tại)
git config core.hooksPath .githooks
chmod +x .githooks/* 2>/dev/null
echo "Da bat git hook tu .githooks (commit-msg, pre-push)"

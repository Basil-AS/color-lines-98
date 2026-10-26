#!/usr/bin/env bash
set -euo pipefail

# Configure only the disposable Ubuntu runner. Its Azure mirror can stall while
# Playwright installs fonts; keep the full dependency set on Ubuntu's own mirror.
printf '%s\n' 'Acquire::Retries "3";' 'Acquire::http::Timeout "30";' 'Acquire::https::Timeout "30";' |
  sudo tee /etc/apt/apt.conf.d/99-color-lines-network >/dev/null
for source_file in /etc/apt/apt-mirrors.txt /etc/apt/sources.list.d/ubuntu.sources; do
  if [[ -f "$source_file" ]]; then
    sudo sed -i 's/azure.archive.ubuntu.com/archive.ubuntu.com/g' "$source_file"
  fi
done
npx playwright install --with-deps chromium

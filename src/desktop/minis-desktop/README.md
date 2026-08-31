# minis-desktop

局域网桌面伴侣：让手机上的 Minis 把 `shell_execute` 发到这台电脑执行。

```sh
python3 minis_desktop.py --port 18765
```

可选：`pip install zeroconf` 后会通过 `_minis-desktop._tcp` 自动被手机发现。
没有 zeroconf 时，在 App 里手动填写 `主机:端口` 即可。

配对成功后 token 存在 `~/.minis-desktop/tokens.json`。
保持「允许此电脑被发现和控制」：不要加 `--no-discover`。

# Setting up firewall for StrategyQuant X on Linux

- Source: <https://strategyquant.com/doc/strategyquant/setting-up-firewall-for-strategyquant-x-on-linux/>
- Section: StrategyQuant X › Installation
- Fetched: 2026-09-04

---

When you are using StrategyQuantX on Linux for the first time and when you are using a firewall you have to allow ports for StrategyQuant X.

**StrategyQuant use these ports:**

```
25,465,587 - SMTP server

443  - Web

80 - Web

8080-8099 - User interface

5050-5059 - User interface
```

## Now, I will show you how to allow ports for the Ubuntu distro and ufw firewall.

**Run these commands for allowing all the ports:**

```
ufw allow 8080:8099/tcp

ufw allow 80

ufw allow 443

ufw allow 25

ufw allow 465

ufw allow 587

ufw allow 5050:5059/tcp
```

**Check firewall status:**

```
ufw status
```

**You should get this output:**

[![](https://strategyquant.com/wp-content/uploads/2021/06/ouput-linux.png)](https://strategyquant.com/wp-content/uploads/2021/06/ouput-linux.png)

Congratulations, should be able to run StrategyQuant X with an enabled firewall on Ubuntu Linux.

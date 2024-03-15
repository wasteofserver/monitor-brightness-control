# Monitor Brightness Control

This is just a quick hacked solution to control Desktop Monitor Brightness.

`Ctrl+Alt+5` decreases brightness   
`Ctrl+Alt+6` increases brightness

That's pretty much all there's currently to this.

### Motivation

[f.lux](https://justgetflux.com/) changes back level and color temperature   
[Redshift](http://jonls.dk/redshift/) changes black level and color temperature   
[Monitorian](https://github.com/emoacht/Monitorian) changes actual brightness

While Monitorian fills most of my needs, **keyboard shortcuts** are a premium feature and while I'm a firm believer in
paying for software, I'm equally **against the subscription** model which unfortunately **is the only way to get
Monitorian**.

Jetbrains has it nailed down with their free fallback perpetual license.

### Current Limitations

- Can't customize brightness steps. It's hardcoded to 5% steps.
- Shortcuts are hardcoded to `Ctrl+Alt+5` and `Ctrl+Alt+6`.
- Doesn't let you have specific brightness levels for each monitor.


- Has only been tested on my current desktop setup - 3 * Dell U2415.

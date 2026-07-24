const fs = require('fs');
const hooks = JSON.parse(fs.readFileSync('plan_pre_hooks.json', 'utf8'));
const researchHook = hooks.activeHooks.find(h => h.capId === 'research' && h.kind === 'step');
console.log(researchHook.fragment.inline);

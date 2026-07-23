const fs = require('fs');
const hooks = JSON.parse(fs.readFileSync('plan_pre_hooks.json', 'utf8'));

// We inject these contributions into the prompt
const plannerContributions = hooks.activeHooks
  .filter(h => h.kind === 'contribution' && h.into === 'planner')
  .map(h => {
    let fragment = h.fragment.inline;
    if (h.capId === 'security') {
      fragment += `\nASVS Level: ${h.configValues.security_asvs_level}\nBlocking threshold: ${h.configValues.security_block_on}`;
    }
    return fragment;
  })
  .join('\n\n');

console.log(plannerContributions);

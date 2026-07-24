const fs = require('fs');
let prompt = fs.readFileSync('research_prompt_template.txt', 'utf8');
const init = JSON.parse(fs.readFileSync('init_plan_phase.json', 'utf8'));
const phaseDesc = fs.readFileSync('/dev/stdin', 'utf8').replace('PHASE_DESC=', '').trim();
const agentSkills = fs.readFileSync('agent_skills_researcher.txt', 'utf8').replace('AGENT_SKILLS_RESEARCHER=', '').trim();

prompt = prompt.replace(/{phase_number}/g, init.phase_number);
prompt = prompt.replace(/{phase_name}/g, init.phase_name);
prompt = prompt.replace(/{context_path}/g, init.context_path);
prompt = prompt.replace(/{requirements_path}/g, init.requirements_path);
prompt = prompt.replace(/{state_path}/g, init.state_path);
prompt = prompt.replace(/\${AGENT_SKILLS_RESEARCHER}/g, agentSkills);
prompt = prompt.replace(/{phase_description}/g, phaseDesc);
prompt = prompt.replace(/{phase_req_ids}/g, init.phase_req_ids);
prompt = prompt.replace(/{phase_dir}/g, init.phase_dir);
prompt = prompt.replace(/{phase_num}/g, init.padded_phase);

fs.writeFileSync('filled_research_prompt.txt', prompt);

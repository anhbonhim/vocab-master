const fs = require('fs');

const plans = [
    {
      "id": "01-02",
      "files_modified": [
        "domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/v6/PyFsrsParityTest.kt"
      ]
    },
    {
      "id": "01-03",
      "files_modified": [
        "domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/v6/Optimizer.kt",
        "domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/v6/OptimizerTest.kt"
      ]
    }
];

let seen_files = {};
let overlap = false;

for (let plan of plans) {
    for (let file of plan.files_modified) {
        if (seen_files[file]) {
            console.log(`Overlap detected: Plan ${plan.id} and Plan ${seen_files[file].id} both modify ${file}`);
            overlap = true;
        } else {
            seen_files[file] = plan;
        }
    }
}
if (!overlap) console.log("No overlap.");

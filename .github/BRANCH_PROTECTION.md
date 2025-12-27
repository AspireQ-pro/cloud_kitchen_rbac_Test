# Branch Protection Configuration
# Apply these settings in GitHub Repository Settings > Branches

## Main Branch Protection Rules:

### Required Status Checks:
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging
- Required checks:
  - `validate-feature`
  - `pre-deployment-checks`

### Restrictions:
- ✅ Restrict pushes that create files larger than 100MB
- ✅ Require pull request reviews before merging
- ✅ Dismiss stale PR approvals when new commits are pushed
- ✅ Require review from code owners
- ✅ Restrict who can dismiss pull request reviews

### Additional Settings:
- ✅ Require signed commits
- ✅ Require linear history
- ✅ Include administrators (enforce rules for admins too)
- ✅ Allow force pushes: NO
- ✅ Allow deletions: NO

## How to Apply:
1. Go to GitHub Repository Settings
2. Click on "Branches" in the left sidebar
3. Click "Add rule" for main branch
4. Configure the above settings
5. Save the protection rule

## Feature Branch Naming Convention:
- feature/JIRA-123-description
- bugfix/JIRA-456-fix-description  
- hotfix/JIRA-789-urgent-fix

This ensures all feature branches are automatically validated before they can be merged to main.
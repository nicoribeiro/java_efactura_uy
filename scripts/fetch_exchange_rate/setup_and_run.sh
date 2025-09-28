#!/bin/bash

# Exit on any error
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Function to compare version numbers
version_ge() {
    # Compare two version numbers (e.g., 3.10.12 >= 3.6.0)
    [ "$(printf '%s\n' "$1" "$2" | sort -V | head -n1)" = "$2" ]
}

# Step 1: Check prerequisites
echo "Checking prerequisites..."

# Check for Python 3
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 is not installed. Please install it (e.g., 'sudo apt install python3' on Ubuntu).${NC}"
    exit 1
fi
PYTHON_VERSION=$(python3 --version | cut -d ' ' -f 2)
if ! version_ge "${PYTHON_VERSION}" "3.6.0"; then
    echo -e "${RED}Error: Python 3.6 or higher is required. Found version ${PYTHON_VERSION}.${NC}"
    exit 1
fi
echo -e "${GREEN}Python 3 found: ${PYTHON_VERSION}${NC}"

# Check for pip
if ! command -v pip3 &> /dev/null; then
    echo -e "${RED}Error: pip3 is not installed. Please install it (e.g., 'sudo apt install python3-pip' on Ubuntu).${NC}"
    exit 1
fi
echo -e "${GREEN}pip3 found${NC}"

# Check for MariaDB client
if ! command -v mysql &> /dev/null; then
    echo -e "${RED}Warning: MariaDB client not found. Ensure MariaDB server is installed and running.${NC}"
    echo "You can install it with 'sudo apt install mariadb-client' on Ubuntu."
else
    echo -e "${GREEN}MariaDB client found${NC}"
fi

# Step 2: Check for existing config.ini or prompt for MariaDB credentials
CONFIG_FILE="config.ini"
if [ -f "$CONFIG_FILE" ]; then
    echo -e "${GREEN}Found existing $CONFIG_FILE. Skipping credential prompts.${NC}"
    # Read credentials from config.ini to use in database setup
    DB_HOST=$(grep '^host=' "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' ')
    DB_USER=$(grep '^user=' "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' ')
    DB_PASS=$(grep '^password=' "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' ')
    DB_NAME=$(grep '^database=' "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' ')
else
    echo "Please provide MariaDB credentials for the 'efactura' database:"
    read -p "MariaDB Host (default: localhost): " DB_HOST
    DB_HOST=${DB_HOST:-localhost}
    read -p "MariaDB Username: " DB_USER
    read -sp "MariaDB Password: " DB_PASS
    echo
    read -p "MariaDB Database (default: efactura): " DB_NAME
    DB_NAME=${DB_NAME:-efactura}

    # Create or update config.ini with MariaDB credentials
    echo "Creating $CONFIG_FILE with MariaDB credentials..."
    cat > "$CONFIG_FILE" << EOL
[Database]
host=$DB_HOST
user=$DB_USER
password=$DB_PASS
database=$DB_NAME
EOL
    echo -e "${GREEN}Credentials written to $CONFIG_FILE${NC}"
fi

# Step 3: Create virtual environment
VENV_DIR="venv"
if [ -d "$VENV_DIR" ]; then
    echo "Virtual environment already exists at $VENV_DIR. Deleting and recreating..."
    rm -rf "$VENV_DIR"
fi
echo "Creating Python virtual environment in $VENV_DIR..."
python3 -m venv "$VENV_DIR"
source "$VENV_DIR/bin/activate"
echo -e "${GREEN}Virtual environment activated${NC}"

# Step 4: Install dependencies from requirements.txt
if [ ! -f "requirements.txt" ]; then
    echo -e "${RED}Error: requirements.txt not found in current directory.${NC}"
    exit 1
fi
echo "Installing dependencies from requirements.txt..."
pip install -r requirements.txt
echo -e "${GREEN}Dependencies installed${NC}"

# Step 6: Run the Python script
SCRIPT_FILE="fetch_exchange_rate.py"
if [ ! -f "$SCRIPT_FILE" ]; then
    echo -e "${RED}Error: $SCRIPT_FILE not found in current directory.${NC}"
    exit 1
fi
echo "Running $SCRIPT_FILE..."
python "$SCRIPT_FILE"
echo -e "${GREEN}Script execution completed${NC}"

# Step 7: Provide verification instructions
echo "To verify the data in MariaDB, run:"
echo "mysql -h $DB_HOST -u $DB_USER -p $DB_NAME -e 'SELECT * FROM tipo_de_cambio;'"

# Deactivate virtual environment
deactivate
echo -e "${GREEN}Setup and execution complete. Virtual environment deactivated.${NC}"
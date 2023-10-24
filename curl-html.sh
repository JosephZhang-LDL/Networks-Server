# Print out the result of the curl command in HTML format
# Usage: ./curl-html.sh <url>
# Example: ./curl-html.sh http://www.example.com
# echo "Running a basic curl command to get the HTML of a website" >> output.txt
# echo "" >> output.txt
# curl -H "Host: www.example.com" http://localhost:8080 >> output.txt
# echo "" >> output.txt
#
# echo "Running a curl command to get the HTML of a website from mobile" >> output.txt
# echo "" >> output.txt
# curl -H "Host: www.example.com" -H "User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1" http://localhost:8080/books/ >> output.txt
# echo "" >> output.txt

curl localhost:8080/index.html && curl localhost:8080/index2.txt && curl localhost:8080/goof.txt && curl localhost:8080/

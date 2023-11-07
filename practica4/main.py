import pandas as pd
import os
import sys
from rdflib import Graph, Namespace

stopWords = ["departamento", "universidad", "zaragoza", 
             ";", "departamento", "facultad",
             "ingenieria", "ciencias",
             "de ", "la ", "y "]


def clean_category(publisher):

    category = publisher

    for word in stopWords:
        category = category.replace(word, '')

    category = category.strip()

    return category



# Example function to generate categories
def generateY(publishers, categories):

    y = []
    
    for publisher in publishers:
        category_name = clean_category(publisher)
        category_id = categories[category_name]

        # Encode in oneHot
        one_hot = [0] * len(categories)
        one_hot[category_id] = 1   

        # Replace this with your actual function for generating categories
        y.append(one_hot)

    # Replace this with your actual function for generating categories
    return y



def generateCategories(publishers):
    
    # Set for the categories
    categories = set()

    for publisher in publishers:
        categories.add(clean_category(publisher))

    # Make an ordered set
    l_categories = list(categories)
    categories = {}

    for i in range(len(l_categories)):
        categories[l_categories[i]] = i

    return categories



# Specify the path to the directory containing the XML files
directory_path = sys.argv[1]

# Initialize an empty list to store the parsed XML data
titles = []
descriptions = []
publishers = []

# Create an RDF Graph
g = Graph()

# Define the Dublin Core (dc) namespace
dc = Namespace("http://purl.org/dc/elements/1.1/")

# Iterate through the files in the directory
for filename in os.listdir(directory_path):
    if filename.endswith(".rdf") or filename.endswith(".xml"):
        file_path = os.path.join(directory_path, filename)

        # Parse the RDF XML file
        g.parse(file_path, format='xml')

        # Extract titles with the Dublin Core (dc) namespace
        for subject, predicate, obj in g.triples((None, dc.title, None)):
            titles.append(obj.lower())

        # Extract description
        for subject, predicate, obj in g.triples((None, dc.description, None)):
            descriptions.append(obj.lower())

        # Extract publisher
        for subject, predicate, obj in g.triples((None, dc.publisher, None)):
            publishers.append(obj.lower())


categories = generateCategories(publishers)

print(categories)

# Generate categories using the external function
ydata = generateY(publishers, categories)

# Convert the list of categories to a string, separated by commas
for i in range(len(ydata)):
    ydata[i] = ','.join(str(e) for e in ydata[i])

# Specify the path for the output CSV file
output_csv_file = 'output_data.csv'

# Write the data to the CSV file
df = pd.DataFrame({'title': titles, 'description': descriptions, 'category': ydata})
df.to_csv(output_csv_file, index=False)

print(f'Data has been written to {output_csv_file}')

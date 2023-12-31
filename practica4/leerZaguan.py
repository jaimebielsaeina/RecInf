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
        #one_hot = [0] * len(categories)
        #one_hot[category_id] = 1   

        y.append(category_id)

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


def leerZaguan(directory_path):

    # Initialize an empty list to store the parsed XML data
    titles = []
    descriptions = []
    publishers = []


    # Define the Dublin Core (dc) namespace
    dc = Namespace("http://purl.org/dc/elements/1.1/")

    print("Loading data...")

    # Iterate through the files in the directory
    for filename in os.listdir(directory_path):
        if filename.endswith(".rdf") or filename.endswith(".xml"):
            file_path = os.path.join(directory_path, filename)

            # Create an RDF Graph
            g = Graph()

            # Parse the RDF XML file
            g.parse(file_path, format='xml')

            # Extract titles with the Dublin Core (dc) namespace
            subject, predicate, obj = next(g.triples((None, dc.title, None)), (None, None, None))

            if obj is None:
                continue

            # Extract description
            subject, predicate, obj = next(g.triples((None, dc.description, None)), (None, None, None))
            
            if obj is None:
                continue

            # Extract publisher
            subject, predicate, obj = next(g.triples((None, dc.publisher, None)), (None, None, None))
            
            if obj is None:
                continue
            
            # Append the objects to the list
            titles.append(obj.lower())
            descriptions.append(obj.lower())
            publishers.append(obj.lower())


    print("Data loaded.")

    print("Procesing data...")
    categories = generateCategories(publishers)

    # Generate categories using the external function
    ydata = generateY(publishers, categories)

    # Convert the list of categories to a string, separated by commas
    #for i in range(len(ydata)):
    #    ydata[i] = ','.join(str(e) for e in ydata[i])

    # Create datos if it doesn't exist
    if not os.path.exists('datos'):
        os.makedirs('datos')

    # Specify the path for the output CSV file
    output_csv_file_train = 'datos/clasificacionEntrenamiento.csv'
    output_csv_file_test = 'datos/clasificacionTest.csv'

    # Create a Pandas DataFrame with the data
    df = pd.DataFrame({'Class Index': ydata, 'Title': titles, 'Description': descriptions})

    # Shuffle the data
    df = df.sample(frac=1).reset_index(drop=True)

    # Separate the 10% of the data for testing
    df_test = df.sample(frac=0.1, random_state=0)
    df = df.drop(df_test.index)

    # Write the data to the CSV file
    df.to_csv(output_csv_file_train, index=False)
    df_test.to_csv(output_csv_file_test, index=False)

    print(f'Data has been written to {output_csv_file_train} and {output_csv_file_test}')
